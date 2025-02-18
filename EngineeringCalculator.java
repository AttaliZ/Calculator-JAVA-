import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.script.ScriptEngineManager;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;

public class EngineeringCalculator extends JFrame implements ActionListener {
    private JTextPane display;
    private JPanel latexPanel;
    private StringBuilder equationBuilder;
    private boolean isResultDisplayed = false;
    private double lastAnswer = 0; // เก็บค่าผลลัพธ์ล่าสุด

    // กำหนดสีและฟอนต์ที่ใช้
    private final Color BACKGROUND_COLOR = new Color(45, 45, 45);
    private final Color BUTTON_COLOR = new Color(60, 60, 60);
    private final Color TEXT_COLOR = Color.WHITE;
    private final Font BUTTON_FONT = new Font("Arial", Font.BOLD, 18);
    private final Font DISPLAY_FONT = new Font("Arial", Font.PLAIN, 32);

    public EngineeringCalculator() {
        setTitle("Casio fx-95MS Emulator");
        setSize(500, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BACKGROUND_COLOR);

        // สร้าง Display สำหรับแสดงสมการและผลลัพธ์
        display = new JTextPane();
        display.setFont(DISPLAY_FONT);
        display.setEditable(true);
        display.setBackground(BACKGROUND_COLOR);
        display.setForeground(TEXT_COLOR);
        display.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        display.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                display.requestFocus();
            }
        });
        add(new JScrollPane(display), BorderLayout.NORTH);

        // LaTeX Display Panel สำหรับแสดงสมการในรูปแบบ LaTeX
        latexPanel = new JPanel();
        latexPanel.setBackground(BACKGROUND_COLOR);
        add(latexPanel, BorderLayout.CENTER);

        // ใช้ StringBuilder ในการเก็บสมการที่ผู้ใช้ป้อนเข้ามา
        equationBuilder = new StringBuilder();

        // สร้างปุ่มบนแผงปุ่มโดยใช้ GridLayout แบบ dynamic (0 แถว หมายถึง จำนวนแถวจะคำนวณเองตามจำนวนปุ่ม)
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(0, 5, 10, 10));
        buttonPanel.setBackground(BACKGROUND_COLOR);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] buttons = {
            "SHIFT", "ALPHA", "MODE", "CLR", "ON",
            "x²", "x³", "x⁻¹", "log", "ln",
            "sin", "cos", "tan", "hyp", "sin⁻¹",
            "cos⁻¹", "tan⁻¹", "Pol(", "nPr", "nCr",
            "7", "8", "9", "DEL", "AC",
            "4", "5", "6", "×", "÷",
            "1", "2", "3", "+", "-",
            "0", ".", "EXP", "Ans", "=",
            "d/dx", "∫", "x^y", "Rnd", "Ran#",
            "a b/c", "lim", "x→0"
        };

        for (String text : buttons) {
            JButton button = createButton(text);
            buttonPanel.add(button);
        }
        add(buttonPanel, BorderLayout.SOUTH);
    }

    // เมธอดสร้างปุ่มด้วยสไตล์ที่กำหนดเอง
    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setBackground(BUTTON_COLOR);
        button.setForeground(TEXT_COLOR);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        button.addActionListener(this);

        // เปลี่ยนสีปุ่มเมื่อ mouse hover
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(new Color(80, 80, 80));
            }
            @Override
            public void mouseExited(MouseEvent evt) {
                button.setBackground(BUTTON_COLOR);
            }
        });
        return button;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

        switch (command) {
            case "AC":
                // ลบสมการทั้งหมด
                equationBuilder.setLength(0);
                display.setText("");
                latexPanel.removeAll();
                latexPanel.revalidate();
                latexPanel.repaint();
                break;
            case "DEL":
                // ลบตัวอักษรสุดท้าย
                if (equationBuilder.length() > 0) {
                    equationBuilder.deleteCharAt(equationBuilder.length() - 1);
                    display.setText(equationBuilder.toString());
                    renderLatex(equationBuilder.toString());
                }
                break;
            case "=":
                calculateResult();
                break;
            case "a b/c":
                appendFraction();
                break;
            case "lim":
                appendToEquation("\\lim_{x \\to 0}");
                break;
            case "x→0":
                appendToEquation("x \\to 0");
                break;
            case "x²":
                applyUnaryOperation("square");
                break;
            case "x³":
                applyUnaryOperation("cube");
                break;
            case "x⁻¹":
                applyUnaryOperation("reciprocal");
                break;
            case "Ans":
                appendAns();
                break;
            case "Rnd":
                generateRandom(false);
                break;
            case "Ran#":
                generateRandom(true);
                break;
            // ปุ่มเหล่านี้สามารถเพิ่มฟังก์ชันเพิ่มเติมในอนาคตได้
            case "SHIFT":
            case "ALPHA":
            case "MODE":
            case "CLR":
            case "ON":
                break;
            default:
                appendToEquation(command);
                break;
        }
    }

    // เมธอดสำหรับเพิ่มข้อความลงในสมการ (ใช้กับตัวเลขและเครื่องหมายพื้นฐาน)
    private void appendToEquation(String text) {
        if (isResultDisplayed) {
            equationBuilder.setLength(0); // ถ้ามีผลลัพธ์แสดงอยู่ ลบสมการเก่าออก
            isResultDisplayed = false;
        }
        equationBuilder.append(text);
        display.setText(equationBuilder.toString());
        renderLatex(equationBuilder.toString());
    }
    
    // เพิ่มค่า Ans (คำตอบล่าสุด) ลงในสมการ
    private void appendAns() {
        if (isResultDisplayed) {
            equationBuilder.setLength(0);
            isResultDisplayed = false;
        }
        equationBuilder.append(lastAnswer);
        display.setText(equationBuilder.toString());
        renderLatex(equationBuilder.toString());
    }
    
    // สร้างตัวเลขสุ่ม
    // ถ้า useRanHash เป็น true ให้คูณด้วย 100 เพื่อให้มีช่วงที่กว้างขึ้น
    private void generateRandom(boolean useRanHash) {
        double rndValue = useRanHash ? Math.random() * 100 : Math.random();
        lastAnswer = rndValue;
        display.setText(String.valueOf(rndValue));
        equationBuilder.setLength(0);
        equationBuilder.append(rndValue);
        isResultDisplayed = true;
        renderLatex(String.valueOf(rndValue));
    }

    // แสดงสมการในรูปแบบ LaTeX
    private void renderLatex(String latex) {
        latexPanel.removeAll();
        try {
            TeXFormula formula = new TeXFormula(latex);
            TeXIcon icon = formula.createTeXIcon(TeXConstants.STYLE_DISPLAY, 20);
            JLabel label = new JLabel(icon);
            label.setForeground(TEXT_COLOR);
            latexPanel.add(label);
        } catch (Exception e) {
            // ถ้าไม่สามารถแปลงเป็น LaTeX ได้ ให้แสดงข้อความธรรมดา
            JLabel label = new JLabel(latex);
            label.setForeground(TEXT_COLOR);
            latexPanel.add(label);
        }
        latexPanel.revalidate();
        latexPanel.repaint();
    }

    // คำนวณผลลัพธ์ของสมการที่ผู้ใช้ป้อน
    private void calculateResult() {
        try {
            String equation = equationBuilder.toString();
            double result;
            if (equation.contains("\\lim")) {
                result = evaluateLimit(equation);
            } else {
                result = evaluateEquation(equation);
            }
            lastAnswer = result;
            display.setText(String.valueOf(result));
            equationBuilder.setLength(0);
            equationBuilder.append(result);
            isResultDisplayed = true;
            renderLatex(String.valueOf(result));
        } catch (Exception ex) {
            display.setText("Error");
            equationBuilder.setLength(0);
            renderLatex("Error");
        }
    }

    // ประเมินสมการโดยใช้ ScriptEngine (หลังจากผ่านการ pre-process)
    private double evaluateEquation(String equation) {
        try {
            String processedEquation = preprocessEquation(equation);
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("JavaScript");
            Object evalResult = engine.eval(processedEquation);
            if (evalResult instanceof Number) {
                return ((Number) evalResult).doubleValue();
            } else {
                throw new RuntimeException("ไม่สามารถคำนวณได้");
            }
        } catch (ScriptException ex) {
            throw new RuntimeException("Error evaluating equation", ex);
        }
    }

    // แปลงสมการให้เป็นรูปแบบที่ JavaScript สามารถคำนวณได้
    private String preprocessEquation(String eq) {
        // แทนที่เครื่องหมายคูณและหาร
        eq = eq.replace("×", "*").replace("÷", "/");
        // แทนที่ฟังก์ชันตรีโกณมิติและลอกอลิทึม
        eq = eq.replaceAll("sin⁻¹", "Math.asin");
        eq = eq.replaceAll("cos⁻¹", "Math.acos");
        eq = eq.replaceAll("tan⁻¹", "Math.atan");
        // ใช้ word boundaries เพื่อไม่ให้แทนที่ส่วนของฟังก์ชันที่แทนที่ไปแล้ว
        eq = eq.replaceAll("\\bsin\\b", "Math.sin");
        eq = eq.replaceAll("\\bcos\\b", "Math.cos");
        eq = eq.replaceAll("\\btan\\b", "Math.tan");
        eq = eq.replaceAll("\\blog\\b", "Math.log10");
        eq = eq.replaceAll("\\bln\\b", "Math.log");
        // แทนที่คำสั่งเศษส่วน LaTeX (\frac{a}{b}) เป็น (a)/(b)
        eq = eq.replaceAll("\\\\frac\\{([^\\}]+)\\}\\{([^\\}]+)\\}", "($1)/($2)");
        return eq;
    }

    // ประเมินลิมิต (ตัวอย่างง่ายสำหรับ x → 0)
    private double evaluateLimit(String equation) {
        String function = equation.replace("\\lim_{x \\to 0}", "").trim();
        function = preprocessEquation(function);
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("JavaScript");
        try {
            // กำหนดค่า x ให้มีค่าเข้าใกล้ 0
            engine.put("x", 0.000001);
            Object evalResult = engine.eval(function);
            if (evalResult instanceof Number) {
                return ((Number) evalResult).doubleValue();
            } else {
                throw new RuntimeException("ไม่สามารถคำนวณได้");
            }
        } catch (ScriptException ex) {
            throw new RuntimeException("Error evaluating function", ex);
        }
    }
    
    // สำหรับเพิ่มเศษส่วนในสมการ (จะแสดงเป็น LaTeX แต่การคำนวณจริงอาจใช้ pre-process แปลงเป็น expression)
    private void appendFraction() {
        appendToEquation("\\frac{ }{ }");
    }
    
    // ประมวลผลฟังก์ชันยูนารี เช่น x², x³, x⁻¹
    private void applyUnaryOperation(String operation) {
        try {
            double value = Double.parseDouble(display.getText());
            double result = 0;
            switch(operation) {
                case "square":
                    result = value * value;
                    break;
                case "cube":
                    result = value * value * value;
                    break;
                case "reciprocal":
                    if (value == 0) {
                        throw new ArithmeticException("Division by zero");
                    }
                    result = 1 / value;
                    break;
            }
            lastAnswer = result;
            display.setText(String.valueOf(result));
            equationBuilder.setLength(0);
            equationBuilder.append(result);
            isResultDisplayed = true;
            renderLatex(String.valueOf(result));
        } catch (NumberFormatException e) {
            display.setText("Error");
            equationBuilder.setLength(0);
            renderLatex("Error");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            EngineeringCalculator calculator = new EngineeringCalculator();
            calculator.setVisible(true);
        });
    }
}
