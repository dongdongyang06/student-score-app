import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * 学生成绩管理（命令行版）
 * 功能：增 / 删 / 改 / 查 / 按分数排序 / 文件读写
 * 运行：javac -encoding UTF-8 StudentScoreApp.java
 *       java StudentScoreApp
 */
public class StudentScoreApp {

    // ==================== 一、学生模型 ====================
    static class Student {
        String id;      // 学号
        String name;    // 姓名
        double score;   // 成绩

        Student(String id, String name, double score) {
            this.id = id;
            this.name = name;
            this.score = score;
        }
    }

    // ==================== 二、管理逻辑 ====================
    static class StudentManager {
        private final List<Student> students = new ArrayList<>();
        private final Path dataFile = Paths.get("scores.csv");

        /** 从文件加载。文件不存在不算错，当作空名单 */
        void load() {
            if (!Files.exists(dataFile)) {
                System.out.println("[提示] 没有找到 scores.csv，从空名单开始。");
                return;
            }
            int ok = 0, bad = 0;
            try (BufferedReader reader = Files.newBufferedReader(dataFile, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    String[] parts = line.split(",");
                    if (parts.length != 3) { bad++; continue; }
                    try {
                        students.add(new Student(parts[0].trim(), parts[1].trim(),
                                Double.parseDouble(parts[2].trim())));
                        ok++;
                    } catch (NumberFormatException e) {
                        bad++;   // 某一行坏了，跳过，不影响其他行
                    }
                }
            } catch (IOException e) {
                System.out.println("[警告] 读取失败：" + e.getMessage());
                return;
            }
            System.out.println("[提示] 已加载 " + ok + " 条记录"
                    + (bad > 0 ? "，跳过 " + bad + " 行脏数据" : ""));
        }

        /** 全量覆盖写入 */
        void save() {
            try (BufferedWriter writer = Files.newBufferedWriter(dataFile, StandardCharsets.UTF_8)) {
                for (Student s : students) {
                    writer.write(s.id + "," + s.name + "," + s.score);
                    writer.newLine();
                }
                System.out.println("[提示] 已保存 " + students.size()
                        + " 条记录到 " + dataFile.toAbsolutePath());
            } catch (IOException e) {
                System.out.println("[错误] 保存失败：" + e.getMessage());
            }
        }

        /** 增：学号重复直接拒绝 */
        void add(String id, String name, double score) {
            if (findById(id) != null) {
                System.out.println("[失败] 学号 " + id + " 已存在。");
                return;
            }
            students.add(new Student(id, name, score));
            System.out.println("[成功] 已添加：" + id + " " + name + " " + score);
        }

        /** 删 */
        boolean delete(String id) {
            Student s = findById(id);
            if (s == null) {
                System.out.println("[失败] 没有学号 " + id + " 的学生。");
                return false;
            }
            students.remove(s);
            System.out.println("[成功] 已删除：" + s.id + " " + s.name);
            return true;
        }

        /** 改：新值为 null 表示这一项不动 */
        boolean update(String id, String newName, Double newScore) {
            Student s = findById(id);
            if (s == null) {
                System.out.println("[失败] 没有学号 " + id + " 的学生。");
                return false;
            }
            if (newName != null && !newName.isEmpty()) s.name = newName;
            if (newScore != null) s.score = newScore;
            System.out.println("[成功] 已更新：" + s.id + " " + s.name + " " + s.score);
            return true;
        }

        /** 按学号精确查找 */
        Student findById(String id) {
            for (Student s : students) {
                if (s.id.equals(id)) return s;
            }
            return null;
        }

        /** 学号或姓名包含关键字即命中 */
        List<Student> search(String keyword) {
            List<Student> result = new ArrayList<>();
            for (Student s : students) {
                if (s.id.contains(keyword) || s.name.contains(keyword)) {
                    result.add(s);
                }
            }
            return result;
        }

        List<Student> all() {
            return students;
        }

        /** 按分数从高到低。返回新列表，不改动原顺序 */
        List<Student> sortedByScoreDesc() {
            List<Student> copy = new ArrayList<>(students);
            // b.score 在前、a.score 在后 = 降序
            copy.sort((a, b) -> Double.compare(b.score, a.score));
            return copy;
        }
    }

    // ==================== 三、命令行界面 ====================
    public static void main(String[] args) {
        StudentManager mgr = new StudentManager();
        mgr.load();
        Scanner sc = new Scanner(System.in);

        while (true) {
            printMenu();
            System.out.print("请选择：");
            String choice = sc.nextLine().trim();   // 全部用 nextLine，避开 nextInt 的换行符问题

            switch (choice) {
                case "1": doAdd(sc, mgr); break;
                case "2": doDelete(sc, mgr); break;
                case "3": doUpdate(sc, mgr); break;
                case "4": doSearch(sc, mgr); break;
                case "5": printTable(mgr.all(), false); break;
                case "6": printTable(mgr.sortedByScoreDesc(), true); break;
                case "0":
                    mgr.save();
                    System.out.println("已退出。");
                    sc.close();
                    return;
                default:
                    System.out.println("没有这个选项，请输入 0-6。");
            }
            System.out.println();
        }
    }

    static void printMenu() {
        System.out.println("========== 学生成绩管理 ==========");
        System.out.println("  1. 添加学生");
        System.out.println("  2. 删除学生");
        System.out.println("  3. 修改学生");
        System.out.println("  4. 查询学生");
        System.out.println("  5. 查看全部");
        System.out.println("  6. 按分数排序（从高到低）");
        System.out.println("  0. 保存并退出");
        System.out.println("==================================");
    }

    static void doAdd(Scanner sc, StudentManager mgr) {
        System.out.print("学号：");
        String id = sc.nextLine().trim();
        if (id.isEmpty()) { System.out.println("学号不能为空。"); return; }
        System.out.print("姓名：");
        String name = sc.nextLine().trim();
        if (name.isEmpty()) { System.out.println("姓名不能为空。"); return; }
        double score = readScore(sc, "成绩：");
        mgr.add(id, name, score);
    }

    static void doDelete(Scanner sc, StudentManager mgr) {
        System.out.print("要删除的学号：");
        mgr.delete(sc.nextLine().trim());
    }

    static void doUpdate(Scanner sc, StudentManager mgr) {
        System.out.print("要修改的学号：");
        String id = sc.nextLine().trim();
        Student s = mgr.findById(id);
        if (s == null) { System.out.println("[失败] 没有学号 " + id + " 的学生。"); return; }

        System.out.println("当前：" + s.name + " " + s.score);
        System.out.print("新姓名（直接回车表示不改）：");
        String name = sc.nextLine().trim();
        System.out.print("新成绩（直接回车表示不改）：");
        String line = sc.nextLine().trim();

        Double score = null;
        if (!line.isEmpty()) {
            try {
                double v = Double.parseDouble(line);
                if (v < 0 || v > 100) {
                    System.out.println("成绩必须在 0-100，本次修改取消。");
                    return;
                }
                score = v;
            } catch (NumberFormatException e) {
                System.out.println("不是数字，本次修改取消。");
                return;
            }
        }
        mgr.update(id, name.isEmpty() ? null : name, score);
    }

    static void doSearch(Scanner sc, StudentManager mgr) {
        System.out.print("输入学号或姓名关键字：");
        String kw = sc.nextLine().trim();
        if (kw.isEmpty()) { System.out.println("关键字不能为空。"); return; }
        List<Student> result = mgr.search(kw);
        if (result.isEmpty()) {
            System.out.println("没有匹配的记录。");
        } else {
            printTable(result, false);
        }
    }

    /** 读一个 0-100 的分数，输错就重问，不让程序崩 */
    static double readScore(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = sc.nextLine().trim();
            try {
                double v = Double.parseDouble(line);
                if (v < 0 || v > 100) {
                    System.out.println("成绩必须在 0-100 之间，请重新输入。");
                    continue;
                }
                return v;
            } catch (NumberFormatException e) {
                System.out.println("这不是数字，请重新输入。");
            }
        }
    }

    /** ranked=true 时多一列排名 */
    static void printTable(List<Student> list, boolean ranked) {
        if (list.isEmpty()) {
            System.out.println("（暂无记录）");
            return;
        }
        System.out.println("--------------------------------------");
        System.out.printf("%-4s %-10s %-8s %6s%n", "", "学号", "姓名", "成绩");
        System.out.println("--------------------------------------");
        int rank = 1;
        for (Student s : list) {
            System.out.printf("%-4s %-10s %-8s %6.1f%n",
                    ranked ? String.valueOf(rank++) : "", s.id, s.name, s.score);
        }
        System.out.println("--------------------------------------");
    }
}