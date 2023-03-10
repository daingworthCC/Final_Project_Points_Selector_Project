import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static junit.framework.Assert.assertTrue;

public class Data {

    /**
     * Level 1: Course ID
     * Level 2: Block number
     * Level 3: number of points
     * Level 4: number of people that put that many points
     */
    private Map<String, Map<String, Map<Integer, Integer>>> coursePointsData;
    private Map<Integer, List<String>> data;

    /**
     * Level 1: Semester
     * Level 2: Course ID
     * Level 3: Block Number
     * Level 4: (0) minimum points, (1) number of seats, (2) demand
     */
    private Map<String, Map<String, Map<String, List<Integer>>>> minMaxPoints;


    /**
     * Level 1: Semester
     * Level 2: Year
     * Level 3: Data
     */
    public Map<String, Map<String, Course>> courseData;

    public XSSFWorkbook workbook;

    /**
     * Level 1: Course ID
     * Level 2: Year & Semester
     * Level 3: Block
     * Level 4: Course info
     */
    HashMap<String, HashMap<String, HashMap<String, Course>>> allData;

    public final String[] semesters = {"2021F", "2021S", "2022F", "2022S", "2023S"};

    public HashMap<String, HashMap<String, HashMap<String, Course>>> getAllData() {
        return allData;
    }

    public Data(String fileLocation) throws IOException, InvalidFormatException {
        data = new HashMap<>();
        coursePointsData = new HashMap<>();
        minMaxPoints = new HashMap<>();
        workbook = new XSSFWorkbook(new File(fileLocation));
        courseData = new HashMap<>();
    }

    public Data() {
        data = new HashMap<>();
        coursePointsData = new HashMap<>();
        minMaxPoints = new HashMap<>();
        courseData = new HashMap<>();
        allData = new HashMap<>();
        allData = readAllFiles();
    }

    public HashMap<String, HashMap<String, HashMap<String, Course>>> readAllFiles() {
        HashMap<String, HashMap<String, HashMap<String, Course>>> output = new HashMap<>();

        for (String semester : semesters) {
            getCourseMinMaxList(semester);
            for (String course : getCourseList(semester)) {
                output.computeIfAbsent(semester, k -> new HashMap<>());
                output.get(semester).computeIfAbsent(charsBtwn(course, 0, 4), k -> new HashMap<>());
                for (String block : minMaxPoints.get(semester).get(charsBtwn(course, 0, 4)).keySet()) {
                    output.get(semester).get(charsBtwn(course, 0, 4)).put(block, readFile(course, semester));
                }
            }

        }

        return output;
    }

    public Course readFile(String course, String semester) {
        int minPoints = minMaxPoints.get(semester).get(charsBtwn(course, 0, 4)).get(charsBtwn(course, 5, course.length())).get(0);
        Course courseUsed = new Course(charsBtwn(course, 0, 4), charsBtwn(course, 5, course.length()), minPoints, getFileData(charsBtwn(course, 0, 4), charsBtwn(course, 5, course.length()), semester).get(charsBtwn(course, 5, course.length())));
        return courseUsed;
    }

    public Map<String, Map<String, Map<Integer, Integer>>> getCoursePointsData() {
        return coursePointsData;
    }

    // Using apache.poi.ooxml.scemas
    // NOT BEING USED
    public Map<Integer, List<String>> readJExcel(int fileNumber)
            throws IOException, InvalidFormatException {

        //FileInputStream file = new FileInputStream(fileLocation);

        Sheet sheet = workbook.getSheetAt(fileNumber);

        data = new HashMap<>();
        int i = 0;
        for (Row row : sheet) {
            data.put(i, new ArrayList<>());
            for (Cell cell : row) {
                switch (cell.getCellType()) {
                    case STRING:
                        data.get(i).add(cell.getStringCellValue());
                        break;
                    case NUMERIC:
                        data.get(i).add(String.valueOf(cell.getNumericCellValue()));
                        break;
                    case BOOLEAN:
                        data.get(i).add(String.valueOf(cell.getBooleanCellValue()));
                        break;
                    case FORMULA:
                        data.get(i).add(String.valueOf(cell.getCellFormula()));
                        break;
                    default:
                        data.get(i).add(" ");
                }
            }
            i++;
        }

        return data;
    }

    // Adds the course points to the Data file
    public void addToCoursePointsData(int fileNumber) throws IOException, InvalidFormatException {
        String name = getCourseID(fileNumber);
        String block = getCourseBlock(fileNumber);
        coursePointsData.put(name, new HashMap<>());
        coursePointsData.get(name).put(block, new HashMap<>());

        Sheet sheet = workbook.getSheetAt(fileNumber);

        Integer j = 0;
        int i = 21;
        while (true) {
            try {
                j = Integer.valueOf((int) sheet.getRow(i).getCell(8).getNumericCellValue());
            } catch (NullPointerException e) {
                break;
            }
            coursePointsData.get(name).get(block).put(j, Integer.valueOf((int) sheet.getRow(i).getCell(7).getNumericCellValue()));
            i++;
        }
    }

    // Adds the course minimum, maximum, etc. points to the Data file
    public void addToMinMaxPointsData(int fileNumber, String semester) throws IOException, InvalidFormatException {
        //Data assurance so I don't put something into nothing
        String name = getCourseID(fileNumber);
        String block = getCourseBlock(fileNumber);
        minMaxPoints.put(semester, new HashMap<>());
        minMaxPoints.get(semester).put(name, new HashMap<>());
        minMaxPoints.get(semester).get(name).put(block, new ArrayList<>());

        Sheet sheet = workbook.getSheetAt(fileNumber);

        //Add the minimum points
        int i;
        try {
            i = (int) sheet.getRow(4).getCell(7).getNumericCellValue();
        } catch (NullPointerException n) {
            i = 0;
        }
        minMaxPoints.get(semester).get(name).get(block).add(0, i);

        //Add the number of seats
        try {
            i = Integer.valueOf(charsBtwn(sheet.getRow(1).getCell(6).getStringCellValue(), 7, 8));
        } catch (NullPointerException n) {
            i = 0;
        }
        minMaxPoints.get(semester).get(name).get(block).add(1, i);

        //Add the seat demand
        try {
            i = Integer.valueOf(charsBtwn(sheet.getRow(2).getCell(6).getStringCellValue(), 8, 9));
        } catch (NullPointerException n) {
            i = 0;
        }
        minMaxPoints.get(semester).get(name).get(block).add(2, i);
    }

    public Map<String, Map<String, Map<String, List<Integer>>>> getMinMaxPoints() {
        return minMaxPoints;
    }

    public String getPrintableMinMaxPointsData() {
        String printData = "";
        for (String course : minMaxPoints.keySet()) {
            printData += ("Course " + course + "\n");
            for (String block : minMaxPoints.get(course).keySet()) {
                printData += block;
                printData += "\t";

                printData += "Minimum points: ";
                printData += minMaxPoints.get(course).get(block).get(0);
                printData += "\t";

                printData += "Seat total: ";
                printData += minMaxPoints.get(course).get(block).get(1);
                printData += "\t";

                printData += "Seat demand: ";
                printData += minMaxPoints.get(course).get(block).get(2);
                printData += "\t";
            }
            printData += "\n";
        }
        return printData;
    }

    public String getPrintData() {
        String printData = "";
        for (Integer key : data.keySet()) {
            printData += (key + " = \t");
            for (Object value : data.get(key).toArray()) {
                printData += (String) value;
                printData += "\t\t";
            }
            printData += "\n";
        }
        return printData;
    }

    //This function allows you to get the names of courses.
    public String getCourseID(int fileNumber) throws IOException, InvalidFormatException {
        String output;

        Sheet sheet = workbook.getSheetAt(fileNumber);

        output = sheet.getRow(1).getCell(2).getStringCellValue();

        String id = "";
        try {
            for (int i = 11; i <= 15; i++) id += output.toCharArray()[i];
        } catch (ArrayIndexOutOfBoundsException a) {
            ;
        }
        return id;
    }

    //This function allows you to get the numbers of courses.
    public String getCourseBlock(int fileNumber) throws IOException, InvalidFormatException {
        String output;

        Sheet sheet = workbook.getSheetAt(fileNumber);

        output = sheet.getRow(1).getCell(2).getStringCellValue();

        String id = "";
        try {
            for (int i = 17; i <= 18; i++) id += output.toCharArray()[i];

            //If it's only one class, return the block. If it is multiple blocks, return the block range
            if (id.toCharArray()[0] == id.toCharArray()[1]) id = String.valueOf(id.charAt(0));
            else {
                String temp = "";
                temp += (id.charAt(0) + "-" + id.charAt(1));
                id = temp;
            }
        } catch (ArrayIndexOutOfBoundsException a) {
            ;
        }
        return id;
    }

    public static String charsBtwn(String input, int start, int end) {
        String id = "";
        try {
            for (int i = start; i <= end; i++) id += input.toCharArray()[i];
        } catch (ArrayIndexOutOfBoundsException a) {
            ;
        }
        return id;
    }

    public Map<Integer, List<String>> getData() {
        return data;
    }

    public static String[] getCourseList(String semester) {
        HashMap<String, HashMap<String, HashMap<Integer, Integer>>> map = null;

        // Deserialize the HashMap
        try {
            FileInputStream fileIn = new FileInputStream("./src/CourseCount/" + semester + ".ser");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            map = ((HashMap<String, HashMap<String, HashMap<Integer, Integer>>>) in.readObject());
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        Set<String> output = new HashSet<>();
        for (String course : map.keySet())
            for (String block : map.get(course).keySet())
                output.add(course + block);

        return output.toArray(String[]::new);
    }
    public static String[] getCourseList1(String semester) {
        HashMap<String, HashMap<String, HashMap<Integer, Integer>>> map = null;

        // Deserialize the HashMap
        try {
            FileInputStream fileIn = new FileInputStream("./src/CourseCount/" + semester + ".ser");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            map = ((HashMap<String, HashMap<String, HashMap<Integer, Integer>>>) in.readObject());
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        Set<String> output = new HashSet<>();
        for (String course : map.keySet())
            for (String block : map.get(course).keySet())
                output.add(course);

        return output.toArray(String[]::new);
    }

    public Map<String, Map<String, List<Integer>>> getCourseMinMaxList(String semester) {
        Map<String, Map<String, List<Integer>>> map = null;

        // Deserialize the HashMap
        try {
            FileInputStream fileIn = new FileInputStream("./src/CourseCount/" + semester + ".ser");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            map = (Map<String, Map<String, List<Integer>>>) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        minMaxPoints.put(semester, map);

        return map;
    }

    /**
     * @param courseID
     * @param block
     * @param semester (format XXXXN with XXXX being the year and N being either S (Spring) or F (Fall)
     * @return map
     */
    public static HashMap<String, HashMap<Integer, Integer>> getFileData(String courseID, String block, String semester) {
        HashMap<String, HashMap<Integer, Integer>> map = null;

        // Deserialize the HashMap
        try (FileInputStream fileIn = new FileInputStream("./src/usableData/" + semester + "/" + courseID + block + ".ser");
             ObjectInputStream in = new ObjectInputStream(fileIn)) {
            map = (HashMap<String, HashMap<Integer, Integer>>) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return map;
    }

    public void sendToPython() {
        FileInputStream inputStream = null;
        Workbook python = null;
        String filePath = "./Final Project/pythonProject/classData.csv";

        try {
            inputStream = new FileInputStream(new File(filePath));
            python = WorkbookFactory.create(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Sheet sheet = python.getSheetAt(0);

        int i = 1;

        for (Row row : sheet) {
            if (row.getRowNum() != 0) {
                sheet.createRow(i);
                i++;
            }
        }

        i = 1;

        for (String semester : allData.keySet()) {
            for (String course : allData.get(semester).keySet()) {
                for (String block : allData.get(semester).get(course).keySet()) {
                    Row row = sheet.createRow(i);
                    row.createCell(0).setCellValue(course);
                    row.createCell(1).setCellValue(block);
                    row.createCell(2).setCellValue(charsBtwn(semester, 0, 4));
                    row.createCell(3).setCellValue(charsBtwn(semester, 5, 5));
                    row.createCell(4).setCellValue(minMaxPoints.get(semester).get(course).get(block).get(2));
                    row.createCell(5).setCellValue(minMaxPoints.get(semester).get(course).get(block).get(1));
                    if (minMaxPoints.get(semester).get(course).get(block).get(2) - minMaxPoints.get(semester).get(course).get(block).get(1) <= 0)
                        row.createCell(6).setCellValue(0);
                    else
                        row.createCell(6).setCellValue(minMaxPoints.get(semester).get(course).get(block).get(2) - minMaxPoints.get(semester).get(course).get(block).get(1));
                    row.createCell(7).setCellValue(minMaxPoints.get(semester).get(course).get(block).get(0));
                    i++;
                }
            }
        }

        try {
            inputStream.close();
            FileOutputStream outputStream = new FileOutputStream(filePath);
            python.write(outputStream);
            python.close();
            outputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


    public String[] getSemesters() {
        return semesters;
    }

    public String[] getCourseList() {
        ArrayList<String> classList= new ArrayList<String>();
        for (String semester : semesters) {
            String[] lst = getCourseList1(semester);
            for (String course : lst) {
                if (classList.contains(course)){}
                else {
                    classList.add(course);
                }
            }
        }
        String [] courseList = classList.toArray(new String[classList.size()]);
        return courseList;
    }
    public String convertSemester(String semester){
        if (semester.endsWith("F")){
            return "Fall " + semester.substring(0,4);
        }
        if (semester.endsWith("S")){
            return "Spring " + semester.substring(0,4);
        }else return semester;
    }

    public String escapeSpecialCharacters(String data) {
        String escapedData = data.replaceAll("\\R", " ");
        if (data.contains(",") || data.contains("\"") || data.contains("'")) {
            data = data.replace("\"", "\"\"");
            escapedData = "\"" + data + "\"";
        }
        return escapedData;
    }

    public String convertToCSV(String[] data) {


        return Stream.of(data)
                .map(this::escapeSpecialCharacters)
                .collect(Collectors.joining(","));
    }

    public static String convertToQuinn(String s) {
        switch (s) {
            case "2021S":
                return "1";
            case "2021F":
                return "2";
            case "2022S":
                return "3";
            case "2022F":
                return "4";
            case "2023S":
                return "5";
            default:
                return "";
        }
    }

    public static String intsBtwn(int start, int end) {
        int i = start;
        String output = "";
        while (i <= end) {
            output += Integer.toString(i);
            i++;
        }
        return output;
    }

    public void sendToPython(String course) throws Exception {

        FileInputStream inputStream = null;
        Workbook python = null;
        String filePath = "src/pythonProject/classData.csv";

        File file = new File(filePath);

        List<String[]> dataLines = new ArrayList<>();

        dataLines.add(new String[] {"","Block","Year","Demand","Limit","Waitlist","MinPoint"});

        for (String semester : allData.keySet()) {
            for (String block : allData.get(semester).get(course).keySet()) {
                dataLines.add(new String[] {
                        charsBtwn(course, 2, 4), //Course ID
                        ((block.length() > 1)
                                ? intsBtwn(block.charAt(0), block.charAt(2))
                                : ((block == "H")
                                    ? "9"
                                    : block)), //Block
                        convertToQuinn(semester),//charsBtwn(semester, 0, 4), //Year
                        //charsBtwn(semester, 4, 4), //Semester
                        String.valueOf(minMaxPoints.get(semester).get(course).get(block).get(2)), //Demand
                        String.valueOf(minMaxPoints.get(semester).get(course).get(block).get(1)), //Supply
                        ((minMaxPoints.get(semester).get(course).get(block).get(2) - minMaxPoints.get(semester).get(course).get(block).get(1) <= 0)
                                ? "0"
                                : String.valueOf(minMaxPoints.get(semester).get(course).get(block).get(2) - minMaxPoints.get(semester).get(course).get(block).get(1))), //Waitlist
                        String.valueOf(minMaxPoints.get(semester).get(course).get(block).get(0)) //Minimum Points
                });
            }
        }

        try (PrintWriter pw = new PrintWriter(file)) {
            dataLines.stream().map(this::convertToCSV).forEach(pw::println);
        }
        assertTrue(file.exists());
    }

    /**
     * It just runs the python code. Run this after running "sendToPython()". Then you can read the returned data.
     * @throws Exception
     */
    public static void runPython() throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder("python", "src/pythonProject/main.py");
        processBuilder.redirectErrorStream(true);

        processBuilder.start();
        System.out.println("Done");
    }


    public static Integer getPrediction() throws Exception {
        Scanner scanner = new Scanner(new File("src/pythonProject/predictedClassPoint.csv"));
        scanner.nextLine();
        String s = scanner.nextLine();
        Integer i = Integer.valueOf((int) Math.round(Double.valueOf(s.split(",")[s.split(",").length - 1]) + 0.5d)); //This complicated list of words converts to Integer.
        return i;
    }

    public static void main(String[] args) throws Exception {
        Data data = new Data();
        data.sendToPython(new Scanner(System.in).nextLine());
        runPython();
        System.out.println(getPrediction());
    }
}
