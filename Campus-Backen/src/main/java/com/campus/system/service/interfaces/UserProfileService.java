package com.campus.system.service.interfaces;

import com.campus.system.vo.AvatarUploadVO;
import com.campus.system.vo.MeVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 个人中心资料业务接口。
 */
public interface UserProfileService {

    MeVO loadProfile();

    AvatarUploadVO uploadAvatar(MultipartFile file) throws Exception;
}
