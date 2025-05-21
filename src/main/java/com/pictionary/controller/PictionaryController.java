package com.pictionary.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

//@RestController
@Controller
public class PictionaryController {

    // 添加一个新方法处理/index2请求
    @RequestMapping(value = "/index2")
    public String index2(Model model) {
        // 可以添加需要的属性
        model.addAttribute("title", "Index 2 Page");
        return "index2";  // 返回templates目录下的index.html

    }

}



