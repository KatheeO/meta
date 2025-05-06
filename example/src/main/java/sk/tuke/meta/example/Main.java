package sk.tuke.meta.example;

import sk.tuke.meta.persistence.GeneratedPersistenceManager;
import sk.tuke.meta.persistence.PersistenceManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException; // Import SQLException
import java.util.List;
import java.util.Optional;

public class Main {
    public static final String DB_PATH = "test.db";

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH)) {

            PersistenceManager manager = new GeneratedPersistenceManager(conn);

            System.out.println("Creating tables...");
            manager.createTables(Person.class, Department.class);
            System.out.println("Tables created (if they didn't exist).");

            exampleOperations(manager);

            System.out.println("Example operations finished.");

        } catch (SQLException e) {
            System.err.println("Database connection or operation failed: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void exampleOperations(PersistenceManager manager) throws SQLException {
        System.out.println("\n--- Starting Example Operations ---");

        // Create and save a Department
        Department development = new Department("Development", "DVLP");
        System.out.println("Saving department: " + development);
        manager.save(development);
        System.out.println("Department saved (ID should now be set): " + development); // Assuming save updates the ID

        // Create and save a Person associated with the Department
        Person hrasko = new Person("Janko", "Hrasko", 30);
        hrasko.setDepartment(development); // Associate Person with the saved Department
        System.out.println("Saving person: " + hrasko);
        manager.save(hrasko);
        System.out.println("Person saved (ID should now be set): " + hrasko); // Assuming save updates the ID

        // Retrieve all Persons
        System.out.println("\nRetrieving all persons...");
        List<Person> persons = manager.getAll(Person.class);
        if (persons.isEmpty()) {
            System.out.println("No persons found in the database.");
        } else {
            for (Person person : persons) {
                System.out.println("Found person: " + person);

                if (person.getDepartment() != null) {
                    System.out.println("  Department: " + person.getDepartment());
                } else {
                    System.out.println("  Department: null");
                }
            }
        }

        // Attempt to retrieve a non-existent Department
        long nonExistentId = 100L; // Use L for long literal
        System.out.println("\nAttempting to retrieve Department with ID: " + nonExistentId);
        Optional<Department> anotherDepartment = manager.get(Department.class, nonExistentId);
        System.out.println("Department with ID " + nonExistentId + " exists: " + anotherDepartment.isPresent());

        if (development.getId() != 0) { // Check if ID was set after save
            System.out.println("\nAttempting to retrieve Department with ID: " + development.getId());
            Optional<Department> retrievedDev = manager.get(Department.class, development.getId());
            if(retrievedDev.isPresent()) {
                System.out.println("Successfully retrieved: " + retrievedDev.get());
            } else {
                System.out.println("Could not retrieve department with ID: " + development.getId());
            }
        }


        System.out.println("\n--- Finished Example Operations ---");
    }
}