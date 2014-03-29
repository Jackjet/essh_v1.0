package com.eryansky.core.aop;

import com.eryansky.common.orm.hibernate.DefaultEntityManager;
import com.eryansky.common.orm.hibernate.HibernateDao;
import com.eryansky.common.utils.StringUtils;
import com.eryansky.common.web.struts2.utils.Struts2Utils;
import com.eryansky.core.security.SecurityConstants;
import com.eryansky.core.security.SecurityUtils;
import com.eryansky.core.security.SessionInfo;
import com.eryansky.entity.sys.Log;
import com.eryansky.entity.sys.state.LogType;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.net.InetAddress;
import java.util.Date;

/**
 * 日志拦截
 */
// 使用@Aspect 定义一个切面类
@Aspect
@Component(value = SecurityConstants.SERVICE_SECURITY_LOGINASPECT)
public class LogAspect {

    private static Logger logger = LoggerFactory.getLogger(LogAspect.class);

    @Autowired
    private DefaultEntityManager defaultEntityManager;

    /**
     * @param point 切入点
     */

    @Around("execution(* com.eryansky.service..*Manager.*(..))")
    public Object logAll(ProceedingJoinPoint point) {
        Object result = null;
        // 执行方法名
        String methodName = point.getSignature().getName();
        String className = point.getTarget().getClass().getSimpleName();
        String userName = null;
        Long start = 0L;
        Long end = 0L;
        String ip = null;
        // 当前用户
        try {
            // 执行方法所消耗的时间
            start = System.currentTimeMillis();
            result = point.proceed();
            end = System.currentTimeMillis();

            // 登录名
            SessionInfo sessionInfo = SecurityUtils.getCurrentSessionInfo();
            if (sessionInfo != null) {
                userName = sessionInfo.getLoginName();
                ip = sessionInfo.getIp();
            } else {
                userName = "系统";
                ip = "127.0.0.1";
                logger.warn("sessionInfo为空.");
            }
        } catch (Throwable e) {
            logger.error(e.getMessage());
//            e.printStackTrace();
        }
        String name = null;
        // 操作范围
        if (className.indexOf("Resource") > -1) {
            name = "资源管理";
        } else if (className.indexOf("Role") > -1) {
            name = "角色管理";
        } else if (className.indexOf("User") > -1) {
            name = "用户管理";
        } else if (className.indexOf("Organ") > -1) {
            name = "机构管理";
        } else {
            name = className;
        }
        // 操作类型
        String opertype = methodName;
        if (StringUtils.isNotBlank(opertype) && (opertype.indexOf("save") > -1 || opertype.indexOf("update") > -1 ||
                opertype.indexOf("delete") > -1 || opertype.indexOf("merge") > -1)) {
            Long time = end - start;
            Log log = new Log();
            log.setType(LogType.operate.getValue());
            log.setLoginName(userName);
            log.setModule(name);
            log.setAction(opertype);
            log.setOperTime(new Date(start));
            log.setActionTime(time.toString());
            log.setIp(ip);
            defaultEntityManager.save(log);
        }
        if(logger.isDebugEnabled()){
            logger.debug("用户:{},操作类：{},操作方法：{},耗时：{}ms.",new Object[]{userName,className,methodName,end - start});
        }
        return result;
    }


}