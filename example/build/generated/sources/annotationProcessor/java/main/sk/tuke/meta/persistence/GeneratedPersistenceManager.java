package sk.tuke.meta.persistence;

import java.sql.Connection;
import sk.tuke.meta.persistence.DAOPersistenceManager;


import sk.tuke.meta.example.Department;
import sk.tuke.meta.example.DepartmentDAO;
import sk.tuke.meta.example.Person;
import sk.tuke.meta.example.PersonDAO;

public class GeneratedPersistenceManager extends DAOPersistenceManager {
    public GeneratedPersistenceManager(Connection connection) {
        super(connection);
            putDAO(Department.class, new DepartmentDAO(this));
            putDAO(Person.class, new PersonDAO(this));
    }
}
