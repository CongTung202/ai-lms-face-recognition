package com.datn.lms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebViewController {

    /**
     * Tự động điều hướng các đường dẫn gốc và admin vào trang EduFace Admin Portal
     */
    @GetMapping({"/", "/admin", "/admin/"})
    public String forwardAdminRoot() {
        return "forward:/admin/index.html";
    }

    @GetMapping({"/portal", "/dashboard"})
    public String forwardPortal() {
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

    @GetMapping({"/admin/profile", "/admin/profile/", "/admin/settings", "/admin/settings/"})
    public String forwardProfile() {
        return "forward:/admin/profile.html";
    }
}
