package com.sky.service.impl;

//import com.alibaba.druid.util.StringUtils;
import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {


    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 指定时间内的营业额
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end) {

        // 用于储存begin,end范围内每天的日期
        List<LocalDate> dateList = new ArrayList<>();

        List<Double> turnoverList = new ArrayList<>();

        dateList.add(begin);

        while(!begin.equals(end)) {
            // 指定日期的后一天
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();
            map.put("beginTime", beginTime);
            map.put("endTime", endTime);
            map.put("status", Orders.COMPLETED);
            Double amount = orderMapper.getSumBymap(map);
            amount = amount == null ? 0.0 : amount;
            turnoverList.add(amount);
        }


        return TurnoverReportVO
                .builder()
                .turnoverList(StringUtils.join(turnoverList, ","))
                .dateList(StringUtils.join(dateList, ","))
                .build();
    }

    /**
     * 用户统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO userStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while(!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> totalUserList = new ArrayList<>();

        List<Integer> newUserList = new ArrayList<>();

        for(LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();
            map.put("endTime", endTime);
            Integer userNum = userMapper.countByMap(map);
            map.put("beginTime", beginTime);
            Integer newUserNum = userMapper.countByMap(map);
            userNum = userNum == null ? 0 : userNum;
            totalUserList.add(userNum);
            newUserList.add(newUserNum);
        }


        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();
    }

    /**
     * 订单统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO orderStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while(!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> orderCountList = new ArrayList<>();
        Integer totalOrderCount = 0;
        List<Integer> vaildOrderCountList = new ArrayList<>();
        Integer VaildOrderCount = 0;

        for(LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();
            map.put("beginTime", beginTime);
            map.put("endTime", endTime);
            Integer orderCount = orderMapper.CountByMap(map);
            orderCountList.add(orderCount);
            totalOrderCount += orderCount;
            map.put("status",Orders.COMPLETED);
            Integer vaildCount = orderMapper.CountByMap(map);
            vaildOrderCountList.add(vaildCount);
            VaildOrderCount += vaildCount;
        }

        return OrderReportVO.builder()
                .orderCountList(StringUtils.join(orderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(VaildOrderCount)
                .validOrderCountList(StringUtils.join(vaildOrderCountList, ","))
                .dateList(StringUtils.join(dateList,","))
                .build();
    }

    /**
     * 销量统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO top10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> goodsSalesDTOList = orderMapper.getByNameNum(beginTime, endTime);

        String nameList = StringUtils.join(goodsSalesDTOList.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList()), ",");

        String numList = StringUtils.join(goodsSalesDTOList.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList()), ",");


        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numList)
                .build();
    }

    /**
     * 导出运营数据报表
     * @param response
     */
    @Override
    public void exportBusinessData(HttpServletResponse response) {
        // 1, 查询数据库，获取营业数据 ... 查询30天的运营数据
        // 前面30天
        LocalDate begin = LocalDate.now().minusDays(30);

        LocalDate end = LocalDate.now().minusDays(1);

        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);

        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        BusinessDataVO businessData = workspaceService.getBusinessData(beginTime, endTime);
        // 2.通过POI写入到Excel文件中
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        try {
            // 基于模板文件创建一个新的Excel文件
            XSSFWorkbook workbook = new XSSFWorkbook(in);

            // 获取表格的sheet页面
            XSSFSheet sheet = workbook.getSheetAt(0);

            // 填充数据--时间
            sheet.getRow(1).getCell(1).setCellValue("时间" + beginTime + "至" + endTime);

            // 获得第4行
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessData.getTurnover());
            row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessData.getNewUsers());

            // 获得第5行
            XSSFRow row1 = sheet.getRow(4);
            row1.getCell(2).setCellValue(businessData.getValidOrderCount());
            row1.getCell(4).setCellValue(businessData.getUnitPrice());

            // 填充我们的明细数据
            for (int i = 0; i < 30; i++) {
                begin = begin.plusDays(i);
                BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(begin, LocalTime.MIN), LocalDateTime.of(begin, LocalTime.MAX));
                // 接收每一行的数据
                XSSFRow row2 = sheet.getRow(7 + i);

                // 获取单元格
                row2.getCell(1).setCellValue(begin.toString());
                row2.getCell(2).setCellValue(businessDataVO.getTurnover());
                row2.getCell(3).setCellValue(businessDataVO.getValidOrderCount());
                row2.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
                row2.getCell(5).setCellValue(businessDataVO.getUnitPrice());
                row2.getCell(6).setCellValue(businessDataVO.getNewUsers());


            }
            // 3.通过输出流将Excel文件下载到客户端浏览器
            ServletOutputStream outputStream = response.getOutputStream();
            workbook.write(outputStream);
            // 关闭资源
            outputStream.flush();
            outputStream.close();
            workbook.close();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}
