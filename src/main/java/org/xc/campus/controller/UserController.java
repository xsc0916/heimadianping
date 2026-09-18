package org.xc.campus.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xc.campus.annotation.RateLimit;
import org.xc.campus.dto.LoginCodeDTO;
import org.xc.campus.enums.LimitDimension;
import org.xc.campus.dto.LoginVO;
import org.xc.campus.dto.UserDTO;
import org.xc.campus.dto.UserUpdateDTO;
import org.xc.campus.service.UserService;
import org.xc.campus.utils.Result;

/**
 * 用户模块接口，对应文档 5.2。
 * Controller 只做三件事：收参数 → 调 Service → 包 Result，不写业务逻辑。
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 5.2.1 发送验证码（mock 模式直接返回验证码，方便联调）。
     *  限流：IP 维度 60 秒最多 5 次 —— 防"一个 IP 换着手机号轰炸短信通道"；
     *  "同一手机号 60 秒一条"由 Service 里的 setIfAbsent 负责，两层维度不同 */
    @RateLimit(limit = 5, windowSeconds = 60, dimension = LimitDimension.IP)
    @PostMapping("/send-code")
    public Result<String> sendCode(@RequestBody @Valid LoginCodeDTO dto) {
        return Result.ok(userService.sendCode(dto.getPhone()));
    }

    /** 5.2.2 登录/注册 */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginCodeDTO dto) {
        return Result.ok(userService.login(dto));
    }

    /** 5.2.3 获取当前用户 */
    @GetMapping("/me")
    public Result<UserDTO> me() {
        return Result.ok(userService.me());
    }

    /** 5.2.4 更新个人信息 */
    @PutMapping("/me")
    public Result<Void> updateProfile(@RequestBody @Valid UserUpdateDTO dto) {
        userService.updateProfile(dto.getNickName(), dto.getIcon());
        return Result.ok();
    }

    /** 5.2.5 登出 */
    @PostMapping("/logout")
    public Result<Void> logout() {
        userService.logout();
        return Result.ok();
    }
}
