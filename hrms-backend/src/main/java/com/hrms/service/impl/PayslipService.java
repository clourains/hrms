package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.entity.Department;
import com.hrms.entity.Employee;
import com.hrms.entity.Salary;
import com.hrms.mapper.DepartmentMapper;
import com.hrms.mapper.EmployeeMapper;
import com.hrms.mapper.SalaryMapper;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * 工资条 PDF 生成（P2-3）
 *
 * 中文字体按常见系统路径依次探测（Windows 黑体/宋体/雅黑、Linux 文泉驿/Noto），
 * 都找不到时抛出明确错误，避免静默输出乱码 PDF。
 */
@Service
public class PayslipService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired
    private SalaryMapper salaryMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    /** 常见中文字体路径（按优先级） */
    private static final String[] CJK_FONT_PATHS = {
            "C:/Windows/Fonts/simhei.ttf",
            "C:/Windows/Fonts/simsun.ttc,0",
            "C:/Windows/Fonts/msyh.ttc,0",
            "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc,0",
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc,0",
            "/System/Library/Fonts/PingFang.ttc,0"
    };

    /**
     * 生成某员工某月工资条 PDF 字节流
     *
     * @throws RuntimeException 薪资记录不存在或无可用中文字体
     */
    public byte[] buildPayslip(Long empId, String salaryMonth) {
        Salary salary = salaryMapper.selectOne(new LambdaQueryWrapper<Salary>()
                .eq(Salary::getEmpId, empId)
                .eq(Salary::getSalaryMonth, salaryMonth));
        if (salary == null) {
            throw new RuntimeException("该员工 " + salaryMonth + " 无薪资记录");
        }
        Employee emp = employeeMapper.selectById(empId);
        String deptName = "";
        if (emp != null && emp.getDeptId() != null) {
            Department dept = departmentMapper.selectById(emp.getDeptId());
            if (dept != null) {
                deptName = dept.getDeptName();
            }
        }

        BaseFont baseFont = resolveCjkFont();
        Font titleFont = new Font(baseFont, 18, Font.BOLD);
        Font headFont = new Font(baseFont, 11, Font.BOLD, Color.WHITE);
        Font labelFont = new Font(baseFont, 10, Font.NORMAL, Color.GRAY);
        Font valueFont = new Font(baseFont, 10);
        Font netFont = new Font(baseFont, 12, Font.BOLD, new Color(0xC0, 0x39, 0x2B));
        Font footFont = new Font(baseFont, 8, Font.NORMAL, Color.GRAY);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A5, 30, 30, 30, 30);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Paragraph title = new Paragraph("工资条", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(4);
            doc.add(title);

            String company = "星跃出海国际贸易有限公司";
            Paragraph companyLine = new Paragraph(company + " · " + salaryMonth, labelFont);
            companyLine.setAlignment(Element.ALIGN_CENTER);
            companyLine.setSpacingAfter(14);
            doc.add(companyLine);

            // 员工信息两列表格
            PdfPTable info = new PdfPTable(new float[]{1, 2, 1, 2});
            info.setWidthPercentage(100);
            addInfoCell(info, "姓名", emp != null ? emp.getName() : "未知", labelFont, valueFont);
            addInfoCell(info, "工号", emp != null && emp.getEmpNo() != null ? emp.getEmpNo() : "-", labelFont, valueFont);
            addInfoCell(info, "部门", deptName, labelFont, valueFont);
            addInfoCell(info, "职位", emp != null && emp.getPosition() != null ? emp.getPosition() : "-", labelFont, valueFont);
            doc.add(info);
            doc.add(new Paragraph(" ", valueFont));

            // 应发项 / 扣款项
            PdfPTable table = new PdfPTable(new float[]{1.2f, 1});
            table.setWidthPercentage(100);
            addHeadRow(table, new String[]{"项目", "金额（元）"}, headFont);
            addMoneyRow(table, "基本工资", salary.getBaseSalary(), valueFont);
            addMoneyRow(table, "绩效奖金", salary.getPerformanceBonus(), valueFont);
            addMoneyRow(table, "加班费", salary.getOvertimePay(), valueFont);
            addMoneyRow(table, "津贴补贴", salary.getAllowance(), valueFont);
            addMoneyRow(table, "社保（个人）", salary.getSocialSecurity(), valueFont);
            addMoneyRow(table, "公积金（个人）", salary.getHousingFund(), valueFont);
            addMoneyRow(table, "个人所得税", salary.getTax(), valueFont);
            addMoneyRow(table, "其他扣款", salary.getOtherDeduction(), valueFont);

            BigDecimal gross = nvl(salary.getBaseSalary()).add(nvl(salary.getPerformanceBonus()))
                    .add(nvl(salary.getOvertimePay())).add(nvl(salary.getAllowance()));
            addMoneyRow(table, "应发合计", gross, valueFont);
            addNetRow(table, "实发工资", salary.getNetSalary(), netFont);
            doc.add(table);

            String remark = salary.getRemark();
            if (remark != null && !remark.isEmpty()) {
                Paragraph remarkPara = new Paragraph("备注：" + remark, labelFont);
                remarkPara.setSpacingBefore(10);
                doc.add(remarkPara);
            }

            Paragraph foot = new Paragraph(
                    "生成时间：" + TIME_FMT.format(LocalDateTime.now()) + "（系统自动生成）", footFont);
            foot.setAlignment(Element.ALIGN_CENTER);
            foot.setSpacingBefore(16);
            doc.add(foot);

            doc.close();
            return out.toByteArray();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("生成工资条失败：" + e.getMessage(), e);
        }
    }

    /**
     * 依次探测系统中文字体；ttc 集合需带 ,0 索引
     */
    private BaseFont resolveCjkFont() {
        for (String path : CJK_FONT_PATHS) {
            try {
                String file = path.split(",")[0];
                if (!new File(file).exists()) {
                    continue;
                }
                return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } catch (Exception ignored) {
                // 尝试下一个字体
            }
        }
        throw new RuntimeException("服务器未找到可用中文字体，无法生成 PDF 工资条");
    }

    private void addInfoCell(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Paragraph(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(labelCell);
        PdfPCell valueCell = new PdfPCell(new Paragraph(value == null || value.isEmpty() ? "-" : value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(valueCell);
    }

    private void addHeadRow(PdfPTable table, String[] texts, Font font) {
        for (String t : texts) {
            PdfPCell cell = new PdfPCell(new Paragraph(t, font));
            cell.setBackgroundColor(new Color(0x40, 0x74, 0xED));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(6);
            table.addCell(cell);
        }
    }

    private void addMoneyRow(PdfPTable table, String label, BigDecimal value, Font font) {
        PdfPCell labelCell = new PdfPCell(new Paragraph(label, font));
        labelCell.setPadding(5);
        table.addCell(labelCell);
        PdfPCell valueCell = new PdfPCell(new Paragraph(money(value), font));
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }

    private void addNetRow(PdfPTable table, String label, BigDecimal value, Font font) {
        PdfPCell labelCell = new PdfPCell(new Paragraph(label, font));
        labelCell.setPadding(7);
        table.addCell(labelCell);
        PdfPCell valueCell = new PdfPCell(new Paragraph(money(value), font));
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(7);
        table.addCell(valueCell);
    }

    private String money(BigDecimal value) {
        return nvl(value).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
