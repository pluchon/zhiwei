package com.campus.system.service.impl;

import com.campus.system.common.enums.NotificationType;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.common.security.CurrentUser;
import com.campus.system.common.security.SecurityUtils;
import com.campus.system.converter.EntityVOConverter;
import com.campus.system.entity.SysRole;
import com.campus.system.entity.SysUser;
import com.campus.system.entity.UserNotification;
import com.campus.system.mapper.SysRoleMapper;
import com.campus.system.mapper.SysUserMapper;
import com.campus.system.mapper.UserNotificationMapper;
import com.campus.system.service.interfaces.OssService;
import com.campus.system.service.interfaces.SsePushService;
import com.campus.system.service.interfaces.UserProfileService;
import com.campus.system.service.profile.AvatarModerationService;
import com.campus.system.service.profile.AvatarReviewResult;
import com.campus.system.vo.AvatarUploadVO;
import com.campus.system.vo.MeVO;
import java.net.URL;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

// 个人中心资料业务实现
@Service
public class UserProfileServiceImpl implements UserProfileService {

    @Autowired
    private SysUserMapper users;

    @Autowired
    private SysRoleMapper roles;

    @Autowired
    private OssService oss;

    @Autowired
    private AvatarModerationService avatarModerationService;

    @Autowired
    private UserNotificationMapper notifications;

    @Autowired
    private SsePushService ssePushService;

    @Override
    public MeVO loadProfile() {
        CurrentUser current = SecurityUtils.current();
        SysUser user = users.selectById(current.userId());
        MeVO vo = new MeVO();
        vo.setUser(EntityVOConverter.toUserVO(user));
        vo.setRoleCode(current.roleCode());
        vo.setRoles(new String[] { current.roleCode() });
        vo.setAvatarUrl(resolveAvatarUrl(user.getAvatar()));
        vo.setRoleLabel(resolveRoleLabel(user.getRoleId()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AvatarUploadVO uploadAvatar(MultipartFile file) throws Exception {
        validateImage(file);
        Long userId = SecurityUtils.current().userId();
        String extension = resolveExtension(file);
        String key = oss.upload(extension, file.getInputStream());
        String imageUrl = oss.signedUrl(key).toString();
        AvatarReviewResult review = avatarModerationService.review(imageUrl, userId);
        if (!review.isApproved()) {
            safeDelete(key);
            notifyRejected(userId, review.getReason());
            throw BusinessException.badRequest("头像审核未通过：" + review.getReason());
        }
        SysUser user = users.selectById(userId);
        String oldAvatar = user.getAvatar();
        user.setAvatar(key);
        users.updateById(user);
        deleteOldAvatarIfNeeded(oldAvatar, key);
        AvatarUploadVO vo = new AvatarUploadVO();
        vo.setAvatarUrl(imageUrl);
        vo.setMessage("头像审核通过，已更新");
        return vo;
    }

    private void validateImage(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty() || file.getSize() > 5L * 1024 * 1024 || ImageIO.read(file.getInputStream()) == null) {
            throw BusinessException.badRequest("仅支持 5MB 内的 JPG/PNG 等有效图片");
        }
    }

    private String resolveExtension(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name != null && name.contains(".")) {
            String extension = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
            if (extension.matches("jpg|jpeg|png|webp|gif")) {
                return extension;
            }
        }
        return "jpg";
    }

    private String resolveAvatarUrl(String avatar) {
        if (!StringUtils.hasText(avatar)) {
            return null;
        }
        if (avatar.startsWith("http://") || avatar.startsWith("https://")) {
            return avatar;
        }
        try {
            URL url = oss.signedUrl(avatar);
            return url == null ? avatar : url.toString();
        } catch (Exception ignored) {
            return avatar;
        }
    }

    private String resolveRoleLabel(Long roleId) {
        SysRole role = roles.selectById(roleId);
        if (role == null) {
            return "未知角色";
        }
        if (StringUtils.hasText(role.getRemark())) {
            return role.getRemark();
        }
        return switch (role.getRoleName()) {
            case "STUDENT" -> "学生";
            case "TEACHER" -> "教师";
            case "REPAIRER" -> "维修师傅";
            case "ADMIN" -> "管理员";
            default -> role.getRoleName();
        };
    }

    private void notifyRejected(Long userId, String reason) {
        UserNotification notification = new UserNotification();
        notification.setReceiverId(userId);
        notification.setNotificationType(NotificationType.AVATAR_REJECTED.getCode());
        notification.setTitle("头像审核未通过");
        notification.setContent("您提交的头像未通过审核，原因：" + reason);
        notification.setIsRead(0);
        notifications.insert(notification);
        ssePushService.pushNotificationChanged(userId);
    }

    private void deleteOldAvatarIfNeeded(String oldAvatar, String newKey) {
        if (!StringUtils.hasText(oldAvatar) || oldAvatar.equals(newKey)) {
            return;
        }
        if (oldAvatar.startsWith("http://") || oldAvatar.startsWith("https://")) {
            return;
        }
        safeDelete(oldAvatar);
    }

    private void safeDelete(String key) {
        try {
            oss.delete(key);
        } catch (Exception ignored) {
            // 清理失败不影响主流程
        }
    }
}
