package com.code.net.test;

import com.alibaba.fastjson.JSONObject;
import com.cui.code.net.util.MailUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * http基础测试
 *
 * @author cuishixiang
 * @date 2018-11-26
 */
public class HttpTest {
    private static final Logger logger = LoggerFactory.getLogger(HttpTest.class);

    private RestTemplate restTemplate = new RestTemplate();

    private String url = "https://s.creditcard.ecitic.com/citiccard/lottery-gateway-pay/pointLottery.do";

    @Test
    public void testGet() {

        ResponseEntity<Object> responseEntity = restTemplate.getForEntity(url, Object.class);
        Object response = responseEntity.getBody();
        System.out.println(response);
    }


    /**
     * 中信信用卡的抽奖接口，这个接口处理的很慢啊，多弄几个线程并发一上来就"0000059——扣减积分失败"，这不行啊……
     * 1.5w分都抽完了，直到最后的"0000060——积分不足"，一个奖都没有中……😌☹️ 再也不相信抽奖了……
     */
    @Test
    public void testPost() {
        ExecutorService executorService = Executors.newWorkStealingPool();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Host", "s.creditcard.ecitic.com");
        httpHeaders.add("Pragma", "no-cache");
        httpHeaders.add("Cache-Control", "no-cache");
        httpHeaders.add("Accept", "application/json");
        httpHeaders.add("Origin", "https://s.creditcard.ecitic.com");
        httpHeaders.add("x-requested-with", "XMLHttpRequest");
        httpHeaders.add("deviceInfo", "undefined");
        httpHeaders.add("User-Agent", "Mozilla/5.0 (Linux; Android 8.0.0; MI 6 Build/OPR1.170623.027; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/63.0.3239.111 Mobile Safari/537.36 DKKJ/4.1.0/DKKJ_TOWER_1.0 DKKJ_TOWER_1.0");
        httpHeaders.add("Content-Type", "application/json; charset=UTF-8");
        httpHeaders.add("Referer", "https://s.creditcard.ecitic.com/citiccard/lotteryfrontend/IntegralLottery.html");
        httpHeaders.add("Accept-Language", "zh-CN,en-US;q=0.9");
        httpHeaders.add("Cookie", "");

        JSONObject request = new JSONObject();
        request.put("actId", "JFCJHD");
        String requestJSON = request.toString();
        System.out.println(requestJSON);

        HttpEntity<Object> httpEntity = new HttpEntity<>(requestJSON, httpHeaders);

        AtomicInteger count = new AtomicInteger(0);
        for (int i = 0; i < 50; i++) {
            Runnable task = () -> {
                String responseBody = restTemplate.postForObject(url, httpEntity, String.class);
                JSONObject jsonObject = JSONObject.parseObject(responseBody);
                int j = count.incrementAndGet();
                if (jsonObject.getString("resultCode").equals("0000006")) {
                    System.out.println(j + "：" + jsonObject.getString("resultDesc"));
                } else {
                    System.out.print(j + "：" + jsonObject.getString("resultCode") + "——");
                    System.out.println(jsonObject.getString("resultDesc"));
                }
            };
            executorService.submit(task);
        }

        executorService.shutdown();
        while (true) {
            if (executorService.isTerminated()) {
                System.out.println("所有线程执行完成");
                break;
            } else {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 京津冀旅游年卡景区预定 测试
     */
    @Test
    public void testBookTicket() {
        // 预登陆后的JSESSIONID
        String JSESSIONID = "自己登陆后的jsessionid";
        BookCardInfo bookCardInfo = new BookCardInfo();
        bookCardInfo.addCardNo("预约卡号1");
        bookCardInfo.addCardNo("预约卡号2");
        bookCardInfo.addCardNo("预约卡号3");
        // 奥林匹克塔：7  延庆打铁花：8   天津的相声：9  蓝调滑雪预约：14    八达岭野生动物园：15  梦幻影院：16   明珠山庄温泉浴场：17
        bookCardInfo.setSubscribeId(SubscribeIdEnum.明珠山庄温泉浴场.getSubscribeId());
        // 预约日期 格式必须是：yyyy-MM-dd
        bookCardInfo.setBookDate("2019-01-21");
        bookCardInfo.setEmailNotice(true);
        // 开启定时抢票的功能，设置开抢的定时时间
        bookCardInfo.setTiming(false);
        LocalDateTime startTime = LocalDateTime.parse("2018-12-31 06:55", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        Instant instant = startTime.atZone(ZoneId.systemDefault()).toInstant();
        bookCardInfo.setTimingStartTime(Date.from(instant));
        // 添加预约截止时间，防止在此时间点之后约到票了但是来不及赶去景点
        LocalDateTime endTime = LocalDateTime.parse("2019-01-21 15:35", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        Instant endInstant = endTime.atZone(ZoneId.systemDefault()).toInstant();
        bookCardInfo.setEndTime(Date.from(endInstant));
        // 校验数据：日期格式对不对？开启定时抢票功能后的开抢时间点是否在当前时间之后？预约截止时间是否在当前时间之后，在开抢定时时间之后？
        // validation()

        System.out.println(new Date());
        int count = 0;
        while (true) {
            if (++count % 100 == 0) {
                System.out.println("retry count：" + count);
                System.out.println(new Date());
            }
            try {
                BookCardInfo bookInfo = getSubscribeCalendarId(bookCardInfo, JSESSIONID);
                if (bookInfo != null) {
                    Integer resultId = lynkBook(bookInfo, JSESSIONID);
                    if (resultId != null) {
                        System.out.println(count + "：预约成功，退出循环");
                        Date date = new Date();
                        System.out.println(date);
                        if (bookCardInfo.isEmailNotice()) {
                            String name = SubscribeIdEnum.getSubscribeIdEnumById(bookCardInfo.getSubscribeId()).name();
                            String subject = MessageFormat.format("景区预约成功——{0}:{1}", name, resultId);
                            String content = MessageFormat.format("预约信息：预约卡号：{0}，预约景区：{1}，预约日期：{2}，预约成功id：{3}，预约成功时间：{4}",
                                    bookCardInfo.getCardInfoList().stream().map(CardInfo::getCardNo).collect(Collectors.joining(";")), name,
                                    bookCardInfo.getBookDate(), resultId, date);
                            MailUtil.sendMailByConfig(subject, content);
                        }
                        return;
                    }
                }
                if (System.currentTimeMillis() >= bookCardInfo.getEndTime().getTime()) {
                    logger.info("当前时间已超过预约截止时间：{}，停止抢票。", bookCardInfo.getEndTime());
                    MailUtil.sendMailByConfig("停止抢票", "当前时间已超过预约截止时间：" + bookCardInfo.getEndTime() + "，停止抢票。如有需要请重新设置后再次启动。");
                    return;
                }

                if (bookCardInfo.isTiming() && System.currentTimeMillis() < bookCardInfo.getTimingStartTime().getTime()) {
                    Thread.sleep(1000 * 60);
                } else {
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                logger.error("抢票异常", e);
                e.printStackTrace();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    /**
     * 获取可预订日期的id
     *
     * @param bookCardInfo 预订卡信息
     * @param JSESSIONID   登陆后的JSESSIONID
     * @return 日期的id
     */
    private BookCardInfo getSubscribeCalendarId(BookCardInfo bookCardInfo, String JSESSIONID) {
        String getSubscribeURL = "http://zglynk.com/ITS/itsApp/goSubscribe.action?subscribeId=" + bookCardInfo.getSubscribeId();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.COOKIE, "JSESSIONID=" + JSESSIONID);
        HttpEntity request = new HttpEntity(httpHeaders);

        String responseString = restTemplate.postForObject(getSubscribeURL, request, String.class);
        // 未登录
        if (responseString.contains("window.open ('/ITS/itsApp/login.jsp','_top')")) {
            lynkLogin(JSESSIONID);
            return null;
        } else if (responseString.contains("<html>\n" +
                "<script>\n" +
                "window.open ('/ITS/itsApp/loginAuthorization.jsp','_top')\n" +
                "</script>\n" +
                "</html>\n")) {
            System.out.println("微信授权已失效，请重新抓取sessionId");
            return null;
        }
        Document document = Jsoup.parse(responseString);
        Elements tables = document.getElementsByClass("ticket-info mart20");
        // 解析日期id
        Element table = tables.get(0);
        Elements trs = table.getElementsByTag("tr");
        for (Element tr : trs) {
            Elements tds = tr.getElementsByTag("td");
            Element date = tds.get(0);
            if (bookCardInfo.getBookDate().equals(date.text())) {
                Element bookTd = tds.get(2);
                String bookText = bookTd.text();
                if (bookText.startsWith("可预约")) {
                    Elements input = bookTd.getElementsByTag("input");
                    String subscribeCalendarId = input.attr("value");
                    bookCardInfo.setSubscribeCalendarId(subscribeCalendarId);
                    break;
                } else {
                    return null;
                }
            }
        }
        //解析cardId
        Element cardTable = tables.get(1);
        Elements cardNoTrs = cardTable.getElementsByTag("tr");
        boolean flag = false;
        for (Element cardNoTr : cardNoTrs) {
            Element td = cardNoTr.getElementsByTag("td").get(1);
            String cardNo = td.text().trim();
            if (bookCardInfo.getCardNoList().contains(cardNo)) {
                // cardNo_XXXXXX
                String name = td.child(0).attr("name");
                String cardId = name.substring(7);
                bookCardInfo.addCardInfo(new CardInfo(cardId, cardNo));
                bookCardInfo.getCardNoList().remove(cardNo);
                flag = true;
            }
        }
        return flag ? bookCardInfo : null;
    }

    /**
     * 京津冀旅游年卡景区预约提交
     */
    private Integer lynkBook(BookCardInfo bookCardInfo, String JSESSIONID) {
        Map<String, String> statusMap = new HashMap<>();
        statusMap.put("1", "预约成功");
        statusMap.put("2", "预约失败，请重试！");
        statusMap.put("3", "超预约规定次数");
        statusMap.put("4", "卡不在允许预约范围内");
        statusMap.put("5", "卡不在允许预约范围内");
        statusMap.put("6", "超过总次数，当天景区预约已满");

        String bookURL = "http://zglynk.com/ITS/itsApp/saveUserSubscribeInfo.action";

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Host", "zglynk.com");
        httpHeaders.add("Pragma", "no-cache");
        httpHeaders.add("Cache-Control", "max-age=0");
        httpHeaders.add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,image/wxpic,image/sharpp,image/apng,image/tpg,*/*;q=0.8");
        httpHeaders.add("Origin", "http://zglynk.com");
        httpHeaders.add("User-Agent", "Mozilla/5.0 (Linux; Android 8.0; MI 6 Build/OPR1.170623.027; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/57.0.2987.132 MQQBrowser/6.2 TBS/044403 Mobile Safari/537.36 MMWEBID/1085 MicroMessenger/6.7.3.1360(0x2607033A) NetType/WIFI Language/zh_CN Process/tools");
        httpHeaders.add("Referer", "http://zglynk.com/ITS/itsApp/goSubscribe.action?subscribeId=" + bookCardInfo.getSubscribeId());
        httpHeaders.add("Accept-Language", "zh-CN,en-US;q=0.8");
        httpHeaders.add("Cookie", "JSESSIONID=" + JSESSIONID);

        MultiValueMap<String, String> parameter = new LinkedMultiValueMap<>();
        parameter.add("subscribeId", bookCardInfo.getSubscribeId());
        parameter.add("subscribeCalendarId", bookCardInfo.getSubscribeCalendarId());
        for (CardInfo cardInfo : bookCardInfo.getCardInfoList()) {
            parameter.add("cardNo_" + cardInfo.getCardId(), cardInfo.getCardNo());
            parameter.add("cardType_" + cardInfo.getCardId(), "1");
            parameter.add("cardId", cardInfo.getCardId() + "#" + cardInfo.getCardNo());
        }

        HttpEntity<Object> request = new HttpEntity<>(parameter, httpHeaders);

        int count = 0;
        for (int i = 0; i < 3; i++) {
            try {
                String responseBody = restTemplate.postForObject(bookURL, request, String.class);
                JSONObject jsonObject = JSONObject.parseObject(responseBody);
                if ("1".equals(jsonObject.getString("status"))) {
                    System.out.println(responseBody);
                    return jsonObject.getInteger("id");
                } else {
                    System.out.println("fail：" + ++count + "——" + responseBody);
                    System.out.println(statusMap.getOrDefault(jsonObject.getString("status"), "预约失败，请重试！"));
                }
                Thread.sleep(100);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;

        // 成功：
        //"{\n" +
        //        "\t\"status\": \"1\",\n" +
        //        "\t\"message\": \"成功\",\n" +
        //        "\t\"id\": \"76198\"\n" +
        //        "}"
    }

    /**
     * 京津冀旅游年卡登陆
     */
    private void lynkLogin(String JSESSIONID) {
        String loginURL = "http://zglynk.com/ITS/itsApp/login.action";
        String userPhone = "手机号";
        String loginPassword = "密码";

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Host", "zglynk.com");
        httpHeaders.add("Pragma", "no-cache");
        httpHeaders.add("Cache-Control", "max-age=0");
        httpHeaders.add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,image/wxpic,image/sharpp,image/apng,image/tpg,*/*;q=0.8");
        httpHeaders.add("Origin", "http://zglynk.com");
        httpHeaders.add("User-Agent", "Mozilla/5.0 (Linux; Android 8.0; MI 6 Build/OPR1.170623.027; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/57.0.2987.132 MQQBrowser/6.2 TBS/044403 Mobile Safari/537.36 MMWEBID/1085 MicroMessenger/6.7.3.1360(0x2607033A) NetType/WIFI Language/zh_CN Process/tools");
        httpHeaders.add("Referer", "http://zglynk.com/ITS/itsApp/login.jsp");
        httpHeaders.add("Accept-Language", "zh-CN,en-US;q=0.8");
        httpHeaders.add(HttpHeaders.COOKIE, "JSESSIONID=" + JSESSIONID);

        MultiValueMap<String, String> parameter = new LinkedMultiValueMap<>();
        parameter.add("userPhone", userPhone);
        parameter.add("loginPassword", loginPassword);
        HttpEntity<Object> request = new HttpEntity<>(parameter, httpHeaders);

        String responseBody = restTemplate.postForObject(loginURL, request, String.class);
        System.out.println(responseBody);
    }

    /**
     * 京津冀旅游年卡景区预定 ：取消
     */
    @Test
    public void testCancelBookTicket() {
        String JSESSIONID = "自己登陆后的jsessionid";
        //要取消的景区
        String cancelName = SubscribeIdEnum.天津中华曲苑相声会馆.name();

        System.out.println(new Date());
        int count = 0;
        while (true) {
            try {
                String bookId = getMySubscribeId(JSESSIONID, cancelName);
                if (bookId != null) {
                    System.out.println("cancel book id：" + bookId);
                    boolean cancelResult = cancelBookTicket(JSESSIONID, bookId);
                    if (cancelResult) {
                        System.out.println(count + "：取消成功，退出循环");
                        System.out.println(new Date());
                        break;
                    }
                }
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取已预订景区的id
     *
     * @param JSESSIONID 登陆后的JSESSIONID
     * @param cancelName 待取消景区的名称
     * @return
     */
    private String getMySubscribeId(String JSESSIONID, String cancelName) {
        String mySubscribeListURL = "http://zglynk.com/ITS/itsApp/goMySubscribeList.action";
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.COOKIE, "JSESSIONID=" + JSESSIONID);
        HttpEntity request = new HttpEntity(httpHeaders);

        String responseString = restTemplate.postForObject(mySubscribeListURL, request, String.class);
        Document document = Jsoup.parse(responseString);

        Element ul = document.select("ul.jq-yu-list.p-top30").first();
        Elements lis = ul.getElementsByTag("li");
        for (Element li : lis) {
            Element aNode = li.getElementsByTag("a").first();
            String ticketTitle = aNode.select("p.font34").text();
            if (ticketTitle.contains(cancelName)) {
                String href = aNode.attr("href");
                String bookId = href.substring("goViewUserSubscribe.action?id=".length());
                return bookId;
            }
        }
        return null;
    }

    /**
     * 取消预订
     *
     * @param JSESSIONID 登陆后的JSESSIONID
     * @param bookId
     */
    private boolean cancelBookTicket(String JSESSIONID, String bookId) {
        String cancelURL = "http://zglynk.com/ITS/itsApp/cancelUserSubscribeInfo.action";

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.COOKIE, "JSESSIONID=" + JSESSIONID);

        MultiValueMap<String, String> parameter = new LinkedMultiValueMap<>();
        parameter.add("id", bookId);
        HttpEntity<Object> request = new HttpEntity<>(parameter, httpHeaders);

        int count = 0;
        for (int i = 0; i < 3; i++) {
            try {
                String responseBody = restTemplate.postForObject(cancelURL, request, String.class);
                JSONObject jsonObject = JSONObject.parseObject(responseBody);
                if ("1".equals(jsonObject.getString("status"))) {
                    System.out.println(responseBody);
                    return true;
                } else {
                    System.out.println("cancel fail：" + ++count + "——" + responseBody);
                }
                Thread.sleep(100);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

}
