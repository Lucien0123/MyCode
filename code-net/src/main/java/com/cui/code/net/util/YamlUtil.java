package com.cui.code.net.util;

import com.cui.code.net.model.BookCardInfo;
import com.cui.code.net.model.SubscribeIdEnum;
import com.cui.code.net.model.hospital.HospitalBookInfo;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

/**
 * yaml文件解析
 *
 * @author cuishixiang
 * @date 2019-02-27
 */
@Slf4j
public class YamlUtil {
    // 京津冀旅游年卡预约信息
    private static final String LY_CONFIG_FILE = "/config/jjjlynkBook.yml";
    private static final String HOSPITAL_CONFIG_FILE = "/config/hospitalBook.yml";
    private static final String HOSPITAL_PROPERTIES_FILE = "/properties/hospital.properties";
    private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static BookCardInfo getBookCardInfo() {
        Yaml yaml = new Yaml(new Constructor(BookCardInfo.class));
        InputStream inputStream = YamlUtil.class.getResourceAsStream(LY_CONFIG_FILE);
        BookCardInfo bookCardInfo = (BookCardInfo) yaml.load(inputStream);

        LocalDateTime startTime = LocalDateTime.parse(bookCardInfo.getTimingStartTimeConfig(), dateTimeFormatter);
        Instant instant = startTime.atZone(ZoneId.systemDefault()).toInstant();
        bookCardInfo.setTimingStartTime(Date.from(instant));

        LocalDateTime endTime = LocalDateTime.parse(bookCardInfo.getEndTimeConfig(), dateTimeFormatter);
        Instant endInstant = endTime.atZone(ZoneId.systemDefault()).toInstant();
        bookCardInfo.setEndTime(Date.from(endInstant));

        SubscribeIdEnum subscribeIdEnum = SubscribeIdEnum.valueOf(bookCardInfo.getSubscribeName());
        bookCardInfo.setSubscribeId(subscribeIdEnum.getSubscribeId());

        return bookCardInfo;
    }

    /**
     * 根据配置文件获取预约挂号信息
     *
     * @return 预约信息
     */
    public static HospitalBookInfo getHospitalBookInfo() {
        Yaml yaml = new Yaml(new Constructor(HospitalBookInfo.class));
        InputStream inputStream = YamlUtil.class.getResourceAsStream(HOSPITAL_CONFIG_FILE);
        HospitalBookInfo hospitalBookInfo = (HospitalBookInfo) yaml.load(inputStream);

        hospitalBookInfo.setHospitalId(getHospitalIdByName(hospitalBookInfo.getHospitalName()));

        return hospitalBookInfo;
    }

    private static Integer getHospitalIdByName(String hospitalName) {
        InputStream inputStream = YamlUtil.class.getResourceAsStream(HOSPITAL_PROPERTIES_FILE);
        Properties properties = new Properties();
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
            properties.load(inputStreamReader);
        } catch (IOException e) {
            log.error("{}文件读取失败，详见异常信息：", HOSPITAL_PROPERTIES_FILE, e);
            throw new RuntimeException(e);
        }

        String hospitalId = properties.getProperty(hospitalName);
        if (hospitalId != null) {
            return Integer.valueOf(hospitalId);
        }

        for (Map.Entry<Object, Object> hospital : properties.entrySet()) {
            if (((String) hospital.getKey()).contains(hospitalName)) {
                return Integer.parseInt((String) hospital.getValue());
            }
        }

        // 找不到的话就直接抛异常吧
        throw new RuntimeException("无法找到" + hospitalName + "对应的医院");
    }

}
