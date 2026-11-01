import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    private static final Path FILE_PATH = Path.of("courses.txt");

    public static void main(String[] args) {
        // Read keyboard input and keep all courses in a resizable list.
        Scanner input = new Scanner(System.in);
        ArrayList<Course> courses = new ArrayList<>();

        // Restore saved courses when the application starts.
        if (Files.exists(FILE_PATH)) {
            loadCourses(courses);
        } else {
            System.out.println("No saved courses found. Starting with an empty list.");
        }

        // Keep displaying the menu until the user selects Exit.
        boolean running = true;
        while (running) {
            System.out.println("\n===== STUDENT COURSE MANAGEMENT SYSTEM =====");
            System.out.println("1. Add Course");
            System.out.println("2. View All Courses");
            System.out.println("3. Search Course by Code");
            System.out.println("4. Compute Total Units");
            System.out.println("5. Save to File");
            System.out.println("6. Load from File");
            System.out.println("7. Exit Program");

            // Read a valid positive integer before processing the menu choice.
            int choice = readPositiveInt(input, "Enter your choice: ");
            switch (choice) {
                // Option 1: Collect course details, prevent duplicates, and save automatically.
                case 1: {
                    String code = readRequiredText(input, "Enter course code: ").toUpperCase();
                    if (searchCourse(courses, code, 0) != null) {
                        System.out.println("A course with this code already exists.");
                        break;
                    }
                    String title = readRequiredText(input, "Enter course title: ");
                    // The pipe character is reserved as a separator in courses.txt.
                    if (code.contains("|") || title.contains("|")) {
                        System.out.println("Course codes and titles must not contain '|'.");
                        break;
                    }
                    int units = readPositiveInt(input, "Enter credit units: ");
                    courses.add(new Course(code, title, units));
                    System.out.println("Course added successfully!");
                    // Save immediately so new courses remain available next time.
                    if (saveCourses(courses)) {
                        System.out.println("Courses saved automatically!");
                    } else {
                        System.out.println("Warning: Course is in memory but was not saved.");
                    }
                    break;
                }
                // Option 2: Display every registered course.
                case 2:
                    if (courses.isEmpty()) {
                        System.out.println("No courses registered yet.");
                    } else {
                        System.out.println("Registered courses:");
                        for (Course course : courses) {
                            System.out.println("Code: " + course.code + ", Title: " + course.title + ", Units: " + course.units);
                        }
                    }
                    break;
                // Option 3: Find a course by its code using recursive search.
                case 3: {
                    String code = readRequiredText(input, "Enter course code to search: ");
                    Course found = searchCourse(courses, code, 0);
                    if (found == null) {
                        System.out.println("Course not found!");
                    } else {
                        System.out.println("Course found! Code: " + found.code + ", Title: " + found.title + ", Units: " + found.units);
                    }
                    break;
                }
                // Option 4: Add up the credit units for all registered courses.
                case 4: {
                    int totalUnits = 0;
                    for (Course course : courses) {
                        totalUnits += course.units;
                    }
                    System.out.println("Total Registered Courses: " + courses.size());
                    System.out.println("Total Credit Units: " + totalUnits);
                    break;
                }
                // Option 5: Allow the user to save courses manually.
                case 5:
                    if (saveCourses(courses)) {
                        System.out.println("Courses saved successfully!");
                    }
                    break;
                // Option 6: Reload the saved list after asking for confirmation.
                case 6:
                    if (!Files.exists(FILE_PATH)) {
                        System.out.println("No saved file found.");
                    } else {
                        System.out.print("Reloading will replace unsaved changes. Continue? (y/n): ");
                        if (input.nextLine().trim().equalsIgnoreCase("y")) {
                            loadCourses(courses);
                        } else {
                            System.out.println("Load cancelled.");
                        }
                    }
                    break;
                // Option 7: End the menu loop and exit.
                case 7:
                    System.out.println("Exiting program...");
                    running = false;
                    break;
                default:
                    System.out.println("Invalid choice! Please select 1-7.");
            }
        }
        input.close();
    }

    // Read whole lines to avoid Scanner's nextInt/nextLine newline problem.
    private static int readPositiveInt(Scanner input, String prompt) {
        while (true) {
            System.out.print(prompt);
            String text = input.nextLine().trim();
            try {
                int number = Integer.parseInt(text);
                if (number > 0) {
                    return number;
                }
            } catch (NumberFormatException e) {
            }
            System.out.println("Please enter a positive whole number.");
        }
    }

    // Reject blank input and remove extra spaces around entered text.
    private static String readRequiredText(Scanner input, String prompt) {
        while (true) {
            System.out.print(prompt);
            String text = input.nextLine().trim();
            if (!text.isEmpty()) {
                return text;
            }
            System.out.println("This field cannot be empty.");
        }
    }

    // Recursive search: stop at the end of the list or return a matching course.
    public static Course searchCourse(ArrayList<Course> courses, String code, int index) {
        if (index >= courses.size()) {
            return null;
        }
        Course course = courses.get(index);
        if (course.code.equalsIgnoreCase(code.trim())) {
            return course;
        }
        return searchCourse(courses, code, index + 1);
    }

    // Return true when saving succeeds and false if an I/O error occurs.
    public static boolean saveCourses(ArrayList<Course> courses) {
        // Build one text record per course: CODE|TITLE|UNITS.
        Path tempFile = Path.of("courses.tmp");
        ArrayList<String> lines = new ArrayList<>();
        for (Course course : courses) {
            lines.add(course.code + "|" + course.title + "|" + course.units);
        }
        try {
            // Write the temporary file before replacing courses.txt.
            Files.write(tempFile, lines, StandardCharsets.UTF_8);
            Files.move(tempFile, FILE_PATH, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            System.out.println("Error saving courses: " + e.getMessage());
            return false;
        }
    }

    // Read saved records, validate them, and replace the in-memory list.
    public static boolean loadCourses(ArrayList<Course> courses) {
        ArrayList<Course> loaded = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(FILE_PATH, StandardCharsets.UTF_8)) {
                if (line.isBlank()) {
                    continue;
                }
                
                String separator = line.contains("|") ? "\\|" : ",";
                String[] parts = line.split(separator, -1);
                if (parts.length != 3) {
                    System.out.println("Skipping invalid record: " + line);
                    continue;
                }
                String code = parts[0].trim().toUpperCase();
                String title = parts[1].trim();
                try {
                    int units = Integer.parseInt(parts[2].trim());
                    // Skip missing fields, invalid units, and duplicate course codes.
                    if (code.isEmpty() || title.isEmpty() || units <= 0 ||
                            code.contains("|") || title.contains("|") ||
                            searchCourse(loaded, code, 0) != null) {
                        System.out.println("Skipping invalid or duplicate record: " + line);
                        continue;
                    }
                    loaded.add(new Course(code, title, units));
                } catch (NumberFormatException e) {
                    System.out.println("Skipping invalid credit units: " + line);
                }
            }
            // Replace existing courses only after the file was read successfully.
            courses.clear();
            courses.addAll(loaded);
            System.out.println("Courses loaded successfully! Total: " + courses.size());
            return true;
        } catch (IOException e) {
            System.out.println("Error loading courses: " + e.getMessage());
            return false;
        }
    }
}

// A Course object stores the code, title, and credit units of one course.
class Course {
    String code;
    String title;
    int units;

    // Initialize the fields of a newly created course.
    Course(String code, String title, int units) {
        this.code = code;
        this.title = title;
        this.units = units;
    }
}
