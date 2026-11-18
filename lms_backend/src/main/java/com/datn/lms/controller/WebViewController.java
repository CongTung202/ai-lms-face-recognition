package com.datn.lms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebViewController {

    /**
     * Trang chủ Landing Page
     */
    @GetMapping({"/"})
    public String forwardLandingPage() {
        return "forward:/index.html";
    }

    /**
     * Cổng Sinh Viên (Student Portal - NTU LMS)
     */
    @GetMapping({"/portal", "/portal/", "/student", "/student/"})
    public String forwardStudentPortal() {
        return "forward:/portal/index.html";
    }

    /**
     * Phòng Học & Ôn Tập Trực Tuyến Sinh Viên
     */
    @GetMapping({"/study", "/study/", "/portal/study", "/portal/study/"})
    public String forwardStudySpace() {
        return "forward:/portal/study.html";
    }

    /**
     * Trang Đăng nhập Học viên / Sinh viên (EduFace Portal)
     */
    @GetMapping({"/login", "/login/", "/portal/login", "/portal/login/", "/student/login", "/student/login/"})
    public String forwardStudentLogin() {
        return "forward:/index.html";
    }

    /**
     * Trang Bảng điều khiển Quản trị (Admin Portal)
     */
    @GetMapping({"/admin", "/admin/", "/dashboard"})
    public String forwardAdminRoot() {
        return "forward:/admin/index.html";
    }

    @GetMapping({"/admin/courses", "/admin/courses/"})
    public String forwardCoursesList() {
        return "forward:/admin/courses/list.html";
    }

    @GetMapping({"/admin/students", "/admin/students/"})
    public String forwardStudentsList() {
        return "forward:/admin/students/list.html";
    }

    @GetMapping({"/admin/faculties", "/admin/faculties/"})
    public String forwardFacultiesList() {
        return "forward:/admin/faculties/list.html";
    }

    @GetMapping({"/admin/classes", "/admin/classes/"})
    public String forwardClassesList() {
        return "forward:/admin/classes/list.html";
    }

    @GetMapping({"/admin/majors", "/admin/majors/"})
    public String forwardMajorsList() {
        return "forward:/admin/majors/list.html";
    }

    @GetMapping({"/admin/announcements", "/admin/announcements/"})
    public String forwardAnnouncementsList() {
        return "forward:/admin/announcements/list.html";
    }

    @GetMapping({"/admin/quizzes", "/admin/quizzes/"})
    public String forwardQuizzesList() {
        return "forward:/admin/quizzes/list.html";
    }

    @GetMapping({"/admin/orders", "/admin/orders/"})
    public String forwardOrdersList() {
        return "forward:/admin/orders/list.html";
    }

    @GetMapping({"/admin/documents", "/admin/documents/"})
    public String forwardDocumentsList() {
        return "forward:/admin/documents/list.html";
    }

    @GetMapping({"/admin/profile", "/admin/profile/", "/admin/settings", "/admin/settings/"})
    public String forwardProfile() {
        return "forward:/admin/profile.html";
    }
}
