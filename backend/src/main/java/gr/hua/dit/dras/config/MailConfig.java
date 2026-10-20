package gr.hua.dit.dras.config;

/* imports */
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Mail configuration to ensure mail credentials such as Google App Passwords are safely sanitized.
 */
@Configuration
public class MailConfig {

    @Bean
    public BeanPostProcessor javaMailSenderPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof JavaMailSenderImpl mailSender) {
                    if (mailSender.getPassword() != null) {
                        mailSender.setPassword(mailSender.getPassword().replaceAll("\\s+", ""));
                    }
                }
                return bean;
            }
        };
    }
}
