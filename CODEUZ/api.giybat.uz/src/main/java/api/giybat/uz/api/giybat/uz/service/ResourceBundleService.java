package api.giybat.uz.api.giybat.uz.service;

import api.giybat.uz.api.giybat.uz.enums.AppLanguage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class ResourceBundleService {

    @Autowired
    private ResourceBundleMessageSource bundleMessage;

    public String getMessage(String code, AppLanguage lang){
        return bundleMessage.getMessage(code, null,new Locale(lang.name()));
    }
}
