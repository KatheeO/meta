//package sk.tuke.meta.persistence.annotations;
//
//import org.aspectj.lang.ProceedingJoinPoint;
//import org.aspectj.lang.annotation.Around;
//import org.aspectj.lang.annotation.Aspect;
//import org.aspectj.lang.annotation.Pointcut;
//import sk.tuke.meta.persistence.DAOPersistenceManager; // manager class
//import sk.tuke.meta.persistence.PersistenceException;
//
//import java.sql.SQLException;
//
//@Aspect
//public class AtomicOperationAspect {
//
//    // Pointcut definition: matches any method annotated with @AtomicPersistenceOperation
//    @Pointcut("@annotation(sk.tuke.meta.persistence.annotations.AtomicPersistenceOperation)")
//    public void atomicOperationMethod() {}
//
//    // Around advice: wraps the execution of methods matched by the pointcut
//    @Around("atomicOperationMethod()")
//    public Object manageAtomicOperation(ProceedingJoinPoint pjp) throws Throwable {
//        // Get the singleton instance of the manager
//        // Ensure DAOPersistenceManager has the getInstance() method and transaction methods
//        DAOPersistenceManager manager = DAOPersistenceManager.getDAOPersistenceManager();
//
//        try {
//            // 1. Begin Transaction
//            manager.beginTransaction();
//            // System.out.println(">>> AtomicOperation: Begin"); // Optional logging
//
//            // 2. Proceed with the original method execution
//            Object result = pjp.proceed();
//
//            // 3. Commit Transaction if successful
//            manager.commitTransaction();
//            // System.out.println(">>> AtomicOperation: Commit"); // Optional logging
//
//            return result; // Return the original method's result
//
//        } catch (Exception e) {
//            // 4. Rollback Transaction on any exception
//            // System.err.println(">>> AtomicOperation: Exception occurred, rolling back: " + e.getMessage()); // Optional logging
//            try {
//                manager.rollbackTransaction();
//            } catch (SQLException rollbackEx) {
//                // Log rollback failure, but prioritize throwing the original exception
//                System.err.println("!!! Critical: Failed to rollback transaction during atomic operation: " + rollbackEx.getMessage());
//                // Optionally chain exceptions: e.addSuppressed(rollbackEx);
//            }
//            // Re-throw the original exception that caused the rollback
//            throw e;
//        }
//        // Note: Cleanup (setAutoCommit(true), remove ThreadLocal) is handled
//        // within commitTransaction() and rollbackTransaction() in the manager.
//    }
//}