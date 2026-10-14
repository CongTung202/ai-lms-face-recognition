-- ====================================================================
-- HỆ THỐNG CƠ SỞ DỮ LIỆU CHẤM CÔNG DOANH NGHIỆP BẰNG FACE AI & GPS
-- HỖ TRỢ: CHECK-IN ĐẦU GIỜ, CHECK-OUT CUỐI GIỜ, GPS GEOFENCING (50-100m)
-- ====================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. ENUM TRẠNG THÁI CHECK-IN & CHECK-OUT
DO $$ BEGIN
    CREATE TYPE check_in_status_type AS ENUM ('ON_TIME', 'LATE');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE check_out_status_type AS ENUM ('ON_TIME', 'EARLY_LEAVE', 'MISSING_CHECKOUT');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE face_angle_type AS ENUM ('straight', 'left', 'right', 'up', 'down');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- --------------------------------------------------------------------
-- 2. BẢNG CHI NHÁNH / DOANH NGHIỆP & TỌA ĐỘ GEOFENCE
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS companies (
    company_id VARCHAR(50) PRIMARY KEY,
    company_name VARCHAR(150) NOT NULL,
    address TEXT,
    latitude DOUBLE PRECISION NOT NULL,   -- Vĩ độ trụ sở công ty (ví dụ: 21.028511)
    longitude DOUBLE PRECISION NOT NULL,  -- Kinh độ trụ sở công ty (ví dụ: 105.804817)
    radius_meters DOUBLE PRECISION DEFAULT 100.0, -- Bán kính cho phép (50m - 100m)
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- --------------------------------------------------------------------
-- 3. BẢNG CA LÀM VIỆC (SHIFTS)
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS shifts (
    shift_id VARCHAR(50) PRIMARY KEY,     -- Ví dụ: CA_HANH_CHINH
    shift_name VARCHAR(100) NOT NULL,     -- Ca Hành Chính
    start_time TIME NOT NULL DEFAULT '08:00:00', -- Giờ vào ca
    end_time TIME NOT NULL DEFAULT '17:30:00',   -- Giờ hết ca
    grace_period_minutes INT DEFAULT 15,  -- Số phút cho phép trễ (15 phút)
    early_leave_minutes INT DEFAULT 0     -- Cho phép về sớm tối đa
);

-- --------------------------------------------------------------------
-- 4. BẢNG NHÂN VIÊN (EMPLOYEES)
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS employees (
    employee_id VARCHAR(50) PRIMARY KEY,  -- Mã NV (NV01, EMP001)
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20),
    department VARCHAR(100) DEFAULT 'Văn phòng',
    position VARCHAR(100) DEFAULT 'Nhân viên',
    company_id VARCHAR(50) REFERENCES companies(company_id),
    shift_id VARCHAR(50) REFERENCES shifts(shift_id) DEFAULT 'CA_HANH_CHINH',
    avatar_url TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- --------------------------------------------------------------------
-- 5. BẢNG VECTOR KHUÔN MẶT ĐA GÓC (FACE EMBEDDINGS)
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS face_embeddings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id VARCHAR(50) NOT NULL REFERENCES employees(employee_id) ON DELETE CASCADE,
    angle_type face_angle_type NOT NULL DEFAULT 'straight',
    embedding REAL[] NOT NULL,            -- Vector 512 số thực
    image_path TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_emp_angle UNIQUE (employee_id, angle_type)
);

CREATE INDEX IF NOT EXISTS idx_face_emp ON face_embeddings(employee_id);

-- --------------------------------------------------------------------
-- 6. BẢNG BẢNG CHẤM CÔNG HÀNG NGÀY (DAILY ATTENDANCE)
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS daily_attendance (
    id BIGSERIAL PRIMARY KEY,
    employee_id VARCHAR(50) NOT NULL REFERENCES employees(employee_id) ON DELETE CASCADE,
    work_date DATE NOT NULL DEFAULT CURRENT_DATE, -- Ngày làm việc
    
    -- CHECK-IN ĐẦU GIỜ
    check_in_time TIMESTAMP WITH TIME ZONE,
    check_in_status check_in_status_type,         -- ON_TIME hoặc LATE
    late_minutes INT DEFAULT 0,                   -- Số phút đi muộn (nếu quá 15p)
    check_in_lat DOUBLE PRECISION,
    check_in_lng DOUBLE PRECISION,
    check_in_distance_meters DOUBLE PRECISION,    -- Khoảng cách GPS tới công ty
    check_in_similarity FLOAT,                    -- Độ khớp khuôn mặt
    check_in_image_url TEXT,
    
    -- CHECK-OUT CUỐI GIỜ
    check_out_time TIMESTAMP WITH TIME ZONE,
    check_out_status check_out_status_type DEFAULT 'MISSING_CHECKOUT', -- ON_TIME, EARLY_LEAVE, MISSING_CHECKOUT
    early_minutes INT DEFAULT 0,                  -- Số phút về sớm
    check_out_lat DOUBLE PRECISION,
    check_out_lng DOUBLE PRECISION,
    check_out_distance_meters DOUBLE PRECISION,
    check_out_similarity FLOAT,
    check_out_image_url TEXT,
    
    -- TỔNG KẾT
    total_work_hours NUMERIC(4, 2) DEFAULT 0.0,  -- Tổng số giờ làm trong ngày
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_emp_work_date UNIQUE (employee_id, work_date)
);

CREATE INDEX IF NOT EXISTS idx_att_date_emp ON daily_attendance(work_date, employee_id);

-- --------------------------------------------------------------------
-- 7. HÀM TÍNH KHOẢNG CÁCH GPS HAVERSINE (ĐƠN VỊ: MÉT)
-- --------------------------------------------------------------------
CREATE OR REPLACE FUNCTION calculate_distance_meters(
    lat1 DOUBLE PRECISION, lon1 DOUBLE PRECISION,
    lat2 DOUBLE PRECISION, lon2 DOUBLE PRECISION
)
RETURNS DOUBLE PRECISION AS $$
DECLARE
    r DOUBLE PRECISION := 6371000; -- Bán kính Trái Đất theo mét
    dlat DOUBLE PRECISION;
    dlon DOUBLE PRECISION;
    a DOUBLE PRECISION;
    c DOUBLE PRECISION;
BEGIN
    dlat := radians(lat2 - lat1);
    dlon := radians(lon2 - lon1);
    a := sin(dlat/2) * sin(dlat/2) + cos(radians(lat1)) * cos(radians(lat2)) * sin(dlon/2) * sin(dlon/2);
    c := 2 * atan2(sqrt(a), sqrt(1-a));
    RETURN r * c;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- --------------------------------------------------------------------
-- DỮ LIỆU MẪU CÔNG TY & CA LÀM VIỆC
-- --------------------------------------------------------------------
INSERT INTO companies (company_id, company_name, address, latitude, longitude, radius_meters)
VALUES ('CP01', 'Trụ sở Doanh nghiệp AI', '123 Đường Công Nghệ, Hà Nội', 21.028511, 105.804817, 100.0)
ON CONFLICT (company_id) DO UPDATE 
SET latitude = EXCLUDED.latitude, longitude = EXCLUDED.longitude, radius_meters = EXCLUDED.radius_meters;

INSERT INTO shifts (shift_id, shift_name, start_time, end_time, grace_period_minutes)
VALUES ('CA_HANH_CHINH', 'Ca Hành Chính (08:00 - 17:30)', '08:00:00', '17:30:00', 15)
ON CONFLICT (shift_id) DO NOTHING;
