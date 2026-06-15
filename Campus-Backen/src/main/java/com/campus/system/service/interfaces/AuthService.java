package com.campus.system.service.interfaces;

import com.campus.system.dto.ActivationCompleteDTO;
import com.campus.system.dto.ActivationStartDTO;
import com.campus.system.dto.CaptchaChallengeDTO;
import com.campus.system.dto.CaptchaTicketDTO;
import com.campus.system.dto.ChangeContactDTO;
import com.campus.system.dto.ChangePasswordDTO;
import com.campus.system.dto.PasswordLoginDTO;
import com.campus.system.dto.PhoneLoginDTO;
import com.campus.system.dto.RecoveryCompleteDTO;
import com.campus.system.dto.RecoveryVerifyDTO;
import com.campus.system.dto.VerificationCodeDTO;
import com.campus.system.vo.ActivationStartVO;
import com.campus.system.vo.CaptchaChallengeVO;
import com.campus.system.vo.CaptchaTicketVO;
import com.campus.system.vo.LoginVO;
import com.campus.system.vo.MeVO;
import com.campus.system.vo.RecoveryTicketVO;
import com.campus.system.vo.VerificationCodeVO;

/**
 * 认证与账号安全业务接口。
 */
public interface AuthService {

    CaptchaChallengeVO captchaChallenge(CaptchaChallengeDTO body);

    CaptchaTicketVO captchaTicket(CaptchaTicketDTO body);

    VerificationCodeVO sendCode(VerificationCodeDTO body);

    LoginVO loginPassword(PasswordLoginDTO body);

    LoginVO loginPhone(PhoneLoginDTO body);

    MeVO me();

    void logout(String token);

    ActivationStartVO activationStart(ActivationStartDTO body);

    void activationComplete(ActivationCompleteDTO body);

    RecoveryTicketVO recoveryVerify(RecoveryVerifyDTO body);

    void recoveryComplete(RecoveryCompleteDTO body);

    void changePassword(ChangePasswordDTO body);

    void changeContact(String type, ChangeContactDTO body);

    void clearSessions(Long userId);
}
