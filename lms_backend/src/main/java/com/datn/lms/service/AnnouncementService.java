package com.datn.lms.service;

import com.datn.lms.dto.AnnouncementDto;
import com.datn.lms.entity.mysql.Announcement;
import com.datn.lms.repository.mysql.AnnouncementRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final ImageCloudService imageCloudService;

    /**
     * Tự động khởi tạo và đồng bộ 7 mục thông báo / hoạt động thực tế vào MySQL khi ứng dụng khởi chạy
     */
    @PostConstruct
    public void initSeedData() {
        try {
            long count = announcementRepository.count();
            if (count > 0) {
                log.info("Cơ sở dữ liệu đã có {} mục thông báo, bỏ qua bước khởi tạo mẫu.", count);
                return;
            }

            log.info("Chưa có thông báo nào trong CSDL. Bắt đầu nén ảnh, upload Cloudinary và lưu 7 sự kiện NTU...");

            List<SeedItem> seedItems = getSeedItems();
            String imagesDir = "d:/DATN/lms_backend/src/main/resources/static/portal/assets/images";

            for (SeedItem item : seedItems) {
                String cloudUrl = null;
                String publicId = null;

                // Thử nén và upload lên Cloudinary từ tệp ảnh nội bộ
                File imgFile = new File(imagesDir, item.localFilename);
                if (imgFile.exists()) {
                    try {
                        Map<String, Object> uploadResult = imageCloudService.uploadFileImage(imgFile, "lms_datn/announcements");
                        cloudUrl = (String) uploadResult.get("secure_url");
                        publicId = (String) uploadResult.get("public_id");
                        log.info("Đã upload ảnh Cloudinary cho sự kiện '{}': {}", item.title, cloudUrl);
                    } catch (Exception e) {
                        log.warn("Không thể upload ảnh lên Cloudinary ({}), sử dụng đường dẫn nội bộ fallback.", e.getMessage());
                    }
                }

                // Nếu upload Cloudinary thành công thì dùng URL Cloudinary, nếu không fallback đường dẫn tĩnh
                String finalImageUrl = (cloudUrl != null && !cloudUrl.isBlank())
                        ? cloudUrl
                        : "assets/images/" + item.localFilename;

                Announcement a = Announcement.builder()
                        .title(item.title)
                        .category(item.category)
                        .categoryName(item.categoryName)
                        .badgeClass(item.badgeClass)
                        .summary(item.summary)
                        .content(item.content)
                        .imageUrl(finalImageUrl)
                        .cloudinaryPublicId(publicId)
                        .eventDate(item.eventDate)
                        .location(item.location)
                        .author("Ban Truyền Thông NTU")
                        .isFeatured(item.isFeatured)
                        .status("PUBLISHED")
                        .viewCount(item.initialViews)
                        .build();

                announcementRepository.save(a);
            }

            log.info("Đã lưu thành công 7 mục thông báo vào CSDL MySQL.");

        } catch (Exception e) {
            log.error("Lỗi trong quá trình khởi tạo dữ liệu thông báo: {}", e.getMessage(), e);
        }
    }

    public List<AnnouncementDto> getAllAnnouncements(String category, String status) {
        List<Announcement> list;
        if (category != null && !category.isBlank() && !"all".equalsIgnoreCase(category)) {
            list = announcementRepository.findByCategoryAndStatusOrderByCreatedAtDesc(
                    category, status != null ? status : "PUBLISHED");
        } else if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
            list = announcementRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            list = announcementRepository.findAllByOrderByCreatedAtDesc();
        }

        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    public Optional<AnnouncementDto> getById(Long id) {
        return announcementRepository.findById(id).map(a -> {
            // Tăng view count
            a.setViewCount(a.getViewCount() + 1);
            announcementRepository.save(a);
            return toDto(a);
        });
    }

    @Transactional
    public AnnouncementDto createAnnouncement(AnnouncementDto dto, MultipartFile file) throws IOException {
        String cloudUrl = dto.getImageUrl();
        String publicId = dto.getCloudinaryPublicId();

        if (file != null && !file.isEmpty()) {
            Map<String, Object> uploadResult = imageCloudService.uploadMultipartImage(file, "lms_datn/announcements");
            cloudUrl = (String) uploadResult.get("secure_url");
            publicId = (String) uploadResult.get("public_id");
        }

        Announcement a = Announcement.builder()
                .title(dto.getTitle())
                .category(dto.getCategory() != null ? dto.getCategory() : "startup")
                .categoryName(dto.getCategoryName() != null ? dto.getCategoryName() : "Sự kiện")
                .badgeClass(dto.getBadgeClass() != null ? dto.getBadgeClass() : "badge-startup")
                .summary(dto.getSummary())
                .content(dto.getContent())
                .imageUrl(cloudUrl)
                .cloudinaryPublicId(publicId)
                .eventDate(dto.getEventDate())
                .location(dto.getLocation())
                .author(dto.getAuthor() != null ? dto.getAuthor() : "Ban Truyền Thông NTU")
                .isFeatured(dto.getIsFeatured() != null && dto.getIsFeatured())
                .status(dto.getStatus() != null ? dto.getStatus() : "PUBLISHED")
                .viewCount(0)
                .build();

        return toDto(announcementRepository.save(a));
    }

    @Transactional
    public AnnouncementDto updateAnnouncement(Long id, AnnouncementDto dto, MultipartFile file) throws IOException {
        Announcement a = announcementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông báo ID: " + id));

        if (file != null && !file.isEmpty()) {
            Map<String, Object> uploadResult = imageCloudService.uploadMultipartImage(file, "lms_datn/announcements");
            a.setImageUrl((String) uploadResult.get("secure_url"));
            a.setCloudinaryPublicId((String) uploadResult.get("public_id"));
        } else if (dto.getImageUrl() != null && !dto.getImageUrl().isBlank()) {
            a.setImageUrl(dto.getImageUrl());
        }

        if (dto.getTitle() != null) a.setTitle(dto.getTitle());
        if (dto.getCategory() != null) a.setCategory(dto.getCategory());
        if (dto.getCategoryName() != null) a.setCategoryName(dto.getCategoryName());
        if (dto.getBadgeClass() != null) a.setBadgeClass(dto.getBadgeClass());
        if (dto.getSummary() != null) a.setSummary(dto.getSummary());
        if (dto.getContent() != null) a.setContent(dto.getContent());
        if (dto.getEventDate() != null) a.setEventDate(dto.getEventDate());
        if (dto.getLocation() != null) a.setLocation(dto.getLocation());
        if (dto.getAuthor() != null) a.setAuthor(dto.getAuthor());
        if (dto.getIsFeatured() != null) a.setIsFeatured(dto.getIsFeatured());
        if (dto.getStatus() != null) a.setStatus(dto.getStatus());

        return toDto(announcementRepository.save(a));
    }

    @Transactional
    public void deleteAnnouncement(Long id) {
        announcementRepository.deleteById(id);
    }

    public Map<String, Object> getStats() {
        long total = announcementRepository.count();
        long published = announcementRepository.countByStatus("PUBLISHED");
        long draft = announcementRepository.countByStatus("DRAFT");
        long archived = announcementRepository.countByStatus("ARCHIVED");

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("published", published);
        stats.put("draft", draft);
        stats.put("archived", archived);
        return stats;
    }

    private AnnouncementDto toDto(Announcement a) {
        return AnnouncementDto.builder()
                .id(a.getId())
                .title(a.getTitle())
                .category(a.getCategory())
                .categoryName(a.getCategoryName())
                .badgeClass(a.getBadgeClass())
                .summary(a.getSummary())
                .content(a.getContent())
                .imageUrl(a.getImageUrl())
                .cloudinaryPublicId(a.getCloudinaryPublicId())
                .eventDate(a.getEventDate())
                .location(a.getLocation())
                .author(a.getAuthor())
                .isFeatured(a.getIsFeatured())
                .status(a.getStatus())
                .viewCount(a.getViewCount())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }

    private static class SeedItem {
        String localFilename;
        String title;
        String category;
        String categoryName;
        String badgeClass;
        String summary;
        String content;
        String eventDate;
        String location;
        boolean isFeatured;
        int initialViews;

        SeedItem(String fn, String t, String c, String cn, String bc, String s, String cnt, String ed, String loc, boolean feat, int views) {
            this.localFilename = fn;
            this.title = t;
            this.category = c;
            this.categoryName = cn;
            this.badgeClass = bc;
            this.summary = s;
            this.content = cnt;
            this.eventDate = ed;
            this.location = loc;
            this.isFeatured = feat;
            this.initialViews = views;
        }
    }

    private List<SeedItem> getSeedItems() {
        List<SeedItem> list = new ArrayList<>();

        list.add(new SeedItem(
                "ntu-startup-arena.jpg",
                "Chung Kết \"NTU Startup Arena\": Bệ Phóng Khởi Nghiệp Công Nghệ AI & Đào Tạo Công Dân Toàn Cầu",
                "startup",
                "Khởi nghiệp Sáng tạo",
                "badge-startup",
                "Đêm chung kết bùng nổ quy tụ những dự án khởi nghiệp công nghệ AI, giải pháp ESG và mô hình kinh doanh số xuất sắc nhất. Ban Giám hiệu NTU cùng đại diện Bộ Giáo dục & Đào tạo đã trao các giải thưởng vinh dự và gói ươm tạo hạt giống cho các nhóm sinh viên tài năng.",
                "<p>Vòng Chung kết Cuộc thi \"Ý tưởng Khởi nghiệp Sáng tạo NTU Startup Arena\" đã diễn ra trong không khí sôi động với sự góp mặt của đại diện Bộ Giáo dục & Đào tạo, Ban Giám hiệu Trường Đại học Nguyễn Trãi cùng các chuyên gia quỹ đầu tư công nghệ hàng đầu.</p><p style=\"margin-top:10px;\">Các dự án xuất sắc được vinh danh bao gồm nền tảng ứng dụng Trí tuệ Nhân tạo trong giáo dục, giải pháp truy xuất nguồn gốc nông sản và mô hình kinh tế tuần hoàn. Các nhóm sinh viên đoạt giải Nhất, Nhì, Ba không chỉ nhận tiền thưởng và cúp vinh dự mà còn được trao gói bảo trợ ươm tạo toàn diện từ Quỹ Hỗ trợ Khởi nghiệp NTU Innovation Fund.</p><p style=\"margin-top:10px;\">Sự kiện là minh chứng rõ nét cho định hướng: <em>\"Tiên phong đào tạo Công dân toàn cầu - Khát vọng thanh xuân, khát vọng khởi nghiệp\"</em> tại Đại học Nguyễn Trãi.</p>",
                "NTU Arena 2026",
                "Khán phòng Đổi mới Sáng tạo NTU",
                true,
                1420
        ));

        list.add(new SeedItem(
                "ntu-the-next-you.jpg",
                "Cuộc Thi \"NTU - The Next You\": Khẳng Định Bản Sắc Cá Nhân & Học Bổng Đối Tác Doanh Nghiệp",
                "startup",
                "Sự kiện & Tài năng",
                "badge-talent",
                "Sân chơi định vị thương hiệu cá nhân giúp sinh viên tự tin khai phóng năng lực, thể hiện kỹ năng thuyết trình và đón nhận những suất học bổng danh giá từ đối tác chiến lược Tập đoàn Atlantic.",
                "<p>Cuộc thi \"NTU - The Next You\" là sân chơi bùng nổ giúp sinh viên rèn giũa kỹ năng xây dựng thương hiệu cá nhân (Personal Branding), phong thái tự tin và năng lực hội nhập trong môi trường đa văn hóa.</p><p style=\"margin-top:10px;\">Tại đêm Gala trao giải, các bạn sinh viên xuất sắc nhất đã nhận được những suất học bổng giá trị cùng lời chúc mừng nồng nhiệt từ đại diện Tập đoàn Atlantic và đối tác quốc tế. Đây là cầu nối vững chắc giúp sinh viên NTU tiếp cận trực tiếp với các nhà tuyển dụng hàng đầu ngay từ năm thứ 2 và thứ 3.</p>",
                "The Next You",
                "Sảnh Hội nghị NTU",
                false,
                980
        ));

        list.add(new SeedItem(
                "ntu-thien-nguyen-kim-bang.jpg",
                "Hành Trình Về Nguồn \"Đền Ơn Đáp Nghĩa\" Tại Trung Tâm Điều Dưỡng Thương Binh Kim Bảng",
                "charity",
                "Hoạt động Cộng đồng",
                "badge-charity",
                "Đoàn Thanh niên và sinh viên tình nguyện NTU trang nghiêm thắt khăn quàng đỏ chào cờ, cất vang lời ca tiếng hát và trao tận tay những phần quà tri ân tới các bác thương bệnh binh tại Hà Nam.",
                "<p>Phát huy truyền thống đạo lý \"Uống nước nhớ nguồn\", Đoàn Thanh niên và Hội Sinh viên Đại học Nguyễn Trãi đã tổ chức chuyến thiện nguyện ý nghĩa đến Trung tâm Điều dưỡng Thương binh Kim Bảng (Hà Nam).</p><p style=\"margin-top:10px;\">Trong không khí trang nghiêm và xúc động, các bạn sinh viên đã thắt khăn quàng đỏ thực hiện nghi thức chào cờ, biểu diễn các tiết mục văn nghệ tri ân và trao tận tay những phần quà, thuốc men cùng số tiền quyên góp cho các thương bệnh binh. Hoạt động góp phần thắp sáng ngọn lửa nhân ái và trách nhiệm xã hội trong lòng mỗi sinh viên NTU.</p>",
                "Tri Ân 30/4",
                "TT Điều Dưỡng Thương Binh Kim Bảng, Hà Nam",
                false,
                1150
        ));

        list.add(new SeedItem(
                "ntu-bong-da-nu-vo-dich.jpg",
                "NTU Women's Cup: Khối Ngoại Ngữ Bản Lĩnh Nâng Cao Cúp Vàng Vô Địch Giải Bóng Đá Nữ",
                "sports",
                "Thể thao & Sức trẻ",
                "badge-sports",
                "Trận chung kết kịch tính và giàu cảm xúc khép lại với chiến thắng vang dội của các cô gái Khối Ngoại Ngữ. Tinh thần đồng đội kiên cường và những pha bóng đẹp mắt đã làm bùng cháy cầu trường NTU.",
                "<p>Giải bóng đá nữ sinh viên NTU Cup đã khép lại với trận chung kết nảy lửa. Đội tuyển nữ Khối Ngoại Ngữ đã thể hiện bản lĩnh kiên cường, chiến thuật linh hoạt và kỹ thuật khéo léo để vượt qua đối thủ mạnh, chính thức nâng cao chiếc cúp vô địch và huy chương vàng danh giá.</p><p style=\"margin-top:10px;\">Nụ cười rạng rỡ, những giọt mồ hôi trên sân cỏ và tinh thần thể thao đoàn kết đã làm nên một mùa giải NTU Women's Cup đầy cảm xúc và đáng nhớ.</p>",
                "NTU Women Cup",
                "Khu Liên Hợp Thể Thao NTU",
                false,
                870
        ));

        list.add(new SeedItem(
                "ntu-vinh-danh-thu-khoa.jpg",
                "Vinh Danh Thủ Khoa Tốt Nghiệp: Tấm Gương Tự Hào Của Sinh Viên Quốc Tế Học NTU",
                "honor",
                "Vinh danh & Học thuật",
                "badge-honor",
                "Nhà trường trang trọng vinh danh Thủ khoa toàn khóa Nguyễn Thị Minh Nguyệt (Lớp K22QTH-HH1, Chuyên ngành Hàn học) với thành tích học tập xuất sắc và giải thưởng nghiên cứu khoa học cấp trường.",
                "<p>Đại học Nguyễn Trãi tự hào vinh danh sinh viên Nguyễn Thị Minh Nguyệt (Lớp K22QTH-HH1, Chuyên ngành Hàn học, Ngành Quốc tế học) - Thủ khoa tốt nghiệp toàn khóa với thành tích học tập xuất sắc và giải thưởng nghiên cứu khoa học cấp Trường.</p><p style=\"margin-top:10px;\">Thầy Hiệu trưởng đã trực tiếp trao tặng bằng khen danh dự, hoa tươi và học bổng khuyến tài cho Minh Nguyệt. Bạn là tấm gương sáng về tinh thần tự học, nỗ lực bền bỉ và khát vọng vươn lên của sinh viên NTU.</p>",
                "Thủ Khoa K22",
                "Hội Trường Lớn NTU",
                false,
                1680
        ));

        list.add(new SeedItem(
                "ntu-bong-da-nam-vo-dich.jpg",
                "Bế Mạc NTU Champions League: Đội Tuyển Nam Nâng Cao Cúp Vàng Mùa Giải Mới",
                "sports",
                "Thể thao & Gắn kết",
                "badge-sports",
                "Giải đấu thường niên thắt chặt tình đoàn kết giữa sinh viên và cán bộ giảng viên khép lại trong tiếng hò reo chiến thắng. Chiếc cúp vô địch là minh chứng cho tinh thần thể thao nhiệt huyết của sức trẻ NTU.",
                "<p>Sau những vòng đấu sôi nổi, giải bóng đá nam toàn trường đã tìm ra chủ nhân của chiếc cúp vô địch danh giá. Trận đấu chung kết thu hút đông đảo thầy cô và hàng trăm sinh viên tới cổ vũ nhiệt tình.</p><p style=\"margin-top:10px;\">Khoảnh khắc nâng cao cúp vàng và cờ vô địch là thành quả xứng đáng cho tinh thần đồng đội gắn kết, kỷ luật tập luyện và ý chí quyết tâm của các cầu thủ.</p>",
                "Champions League",
                "Sân Bóng Trung Tâm NTU",
                false,
                760
        ));

        list.add(new SeedItem(
                "ntu-dai-le-tot-nghiep.jpg",
                "Đại Lễ Trao Bằng Tốt Nghiệp: Rực Rỡ Khoảnh Khắc Tung Mũ Chào Đón Thế Hệ Công Dân Toàn Cầu",
                "honor",
                "Lễ Tốt Nghiệp NTU",
                "badge-grad",
                "Hàng trăm chiếc mũ cử nhân đồng loạt bay lên trong khán phòng trang trọng đánh dấu cột mốc trưởng thành thiêng liêng. Thế hệ Tân cử nhân, Tân kỹ sư NTU sẵn sàng hội nhập và cất cánh trên trường quốc tế.",
                "<p>Giây phút hàng trăm chiếc mũ cử nhân đồng loạt tung bay trong không gian trang trọng của nhà hát đánh dấu cột mốc trưởng thành thiêng liêng của các Tân cử nhân, Tân kỹ sư Đại học Nguyễn Trãi.</p><p style=\"margin-top:10px;\">Mang theo hành trang tri thức thực tiễn, ngoại ngữ vững vàng và tinh thần khởi nghiệp đổi mới sáng tạo, thế hệ sinh viên tốt nghiệp NTU tự tin bước ra thế giới, sẵn sàng cống hiến cho xã hội và khẳng định giá trị bản thân trên thị trường lao động toàn cầu.</p>",
                "Đại Lễ Tốt Nghiệp",
                "Trung Tâm Hội Nghị Quốc Tế",
                true,
                2350
        ));

        return list;
    }
}

