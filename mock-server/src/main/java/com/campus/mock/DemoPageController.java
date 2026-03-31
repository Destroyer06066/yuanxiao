package com.campus.mock;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 演示页面路由控制器
 */
@Controller
public class DemoPageController {

    /**
     * 演示首页 /demo/ → /demo/index.html
     */
    @GetMapping({"/demo", "/demo/"})
    public String demoIndex() {
        return "redirect:/demo/index.html";
    }
}
