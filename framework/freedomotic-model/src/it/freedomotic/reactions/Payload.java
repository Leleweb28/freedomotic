/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.freedomotic.reactions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Enrico
 */
public final class Payload implements Serializable {

    List<Statement> payload = new ArrayList<Statement>();

    public void addStatement(String logical,
            String attribute,
            String operand,
            String value) throws NullPointerException {
        enqueueStatement(new Statement().create(logical, attribute, operand, value));
    }

    public void addStatement(String attribute, String value) {
        enqueueStatement(new Statement().create(Statement.AND, attribute, Statement.EQUALS, value));
    }

    public void addStatement(String attribute, int value) {
        enqueueStatement(new Statement().create(Statement.AND, attribute, Statement.EQUALS, Integer.toString(value)));
    }

    public void enqueueStatement(Statement s) {
        if (s != null && !payload.contains(s)) {
            payload.add(s);
        }
    }

    public int size() {
        return payload.size();
    }

    @Override
    public boolean equals(Object obj) {
        boolean payloadConsistence = true;
        if (obj instanceof Payload) {
            Payload eventPayload = (Payload) obj;
            Iterator it = payload.iterator();
            final boolean precedingCheckResult = true;
            //check all statement for consistency
            while (it.hasNext()) {
                Statement trigger = (Statement) it.next();
                for (Statement eventStatement : eventPayload.getStatements(trigger.attribute)) {
                    /*
                     * TODO: waring, supports only operand equal in event
                     * compared to equal, morethen, lessthen in triggers.
                     * Refacor with a strategy pattern.
                     */
                    if (eventStatement != null) {
                        //is setting a value must be not used to filter
                        if (trigger.logical.equalsIgnoreCase("SET")) {
                            return true;
                        } else {
                            boolean isStatementConsistent = isStatementConsistent(trigger.operand, trigger.value, eventStatement.value);
                            if (trigger.getLogical().equalsIgnoreCase("AND")) {
                                payloadConsistence = payloadConsistence && isStatementConsistent; //true AND true; false AND true; false AND false; true AND false
                            } else {
                                if (trigger.getLogical().equalsIgnoreCase("OR")) {
                                    payloadConsistence = payloadConsistence || isStatementConsistent;
                                }
                            }
                        }
                    }
                }
            }
        } else {
            payloadConsistence = false;
        }
        return payloadConsistence;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + (this.payload != null ? this.payload.hashCode() : 0);
        return hash;
    }

    private static boolean isStatementConsistent(String triggerOperand, String triggerValue, String eventValue) {
        if (triggerOperand.equalsIgnoreCase(Statement.EQUALS)) { //event operand="EQUALS", trigger operand="EQUALS"
            if (triggerValue.equalsIgnoreCase(eventValue)
                    || (triggerValue.equals(Statement.ANY))) {
                return true;
            }
        }

        if (triggerOperand.equals(Statement.REGEX)) { //event operand="EQUALS", trigger operand="REGEX"
            Pattern pattern = Pattern.compile(triggerValue);
            Matcher matcher = pattern.matcher(eventValue);
            if (matcher.matches()) {
                return true;
            } else {
                return false;
            }
        }

        //applies only to integer values
        if (triggerOperand.equals(Statement.GREATER_THEN)) { //event operand="EQUALS", trigger operand="GREATER_THEN"
            try {
                Integer intReactionValue = new Integer(triggerValue);
                Integer intEventValue = new Integer(eventValue);
                if (intEventValue > intReactionValue) {
                    return true;
                } else {
                    return false;
                }
            } catch (NumberFormatException numberFormatException) {
                new RuntimeException(Statement.GREATER_THEN.toString() + " operator can be applied only to integer values");
                return false;
            }

        }
        if (triggerOperand.equals(Statement.LESS_THEN)) { //event operand="EQUALS", trigger operand="LESS_THEN"
            try {
                Integer intReactionValue = new Integer(triggerValue);
                Integer intEventValue = new Integer(eventValue);
                if (intEventValue < intReactionValue) {
                    return true;
                } else {
                    return false;
                }
            } catch (NumberFormatException numberFormatException) {
                //is not a number
                new RuntimeException(Statement.LESS_THEN.toString() + " operator can be applied only to integer values");
                return false;
            }
        }
        //applies only to integer values
        if (triggerOperand.equals(Statement.GREATER_EQUAL_THEN)) { //event operand="EQUALS", trigger operand="GREATER_THEN"
            try {
                Integer intReactionValue = new Integer(triggerValue);
                Integer intEventValue = new Integer(eventValue);
                if (intEventValue >= intReactionValue) {
                    return true;
                } else {
                    return false;
                }
            } catch (NumberFormatException numberFormatException) {
                new RuntimeException(Statement.GREATER_EQUAL_THEN.toString() + " operator can be applied only to integer values");
                return false;
            }

        }
        if (triggerOperand.equals(Statement.LESS_EQUAL_THEN)) { //event operand="EQUALS", trigger operand="LESS_THEN"
            try {
                Integer intReactionValue = new Integer(triggerValue);
                Integer intEventValue = new Integer(eventValue);
                if (intEventValue <= intReactionValue) {
                    return true;
                } else {
                    return false;
                }
            } catch (NumberFormatException numberFormatException) {
                //is not a number
                new RuntimeException(Statement.LESS_EQUAL_THEN.toString() + " operator can be applied only to integer values");
                return false;
            }
        }
        return false;
    }

//    //ERROR: the attribut can have multiple instances in a trigger
    public ArrayList<Statement> getStatements(String attribute) {
        ArrayList<Statement> statements = new ArrayList<Statement>();
        for (Statement i : payload) {
            if (i.getAttribute().equalsIgnoreCase(attribute)) {
                statements.add(i);
            }
        }
        return statements;
    }

    public Iterator iterator() {
        return payload.iterator();
    }

    public void merge(Payload anotherPayload) {
        payload.addAll(anotherPayload.payload);
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        Iterator it = payload.iterator();
        while (it.hasNext()) {
            Statement s = (Statement) it.next();
            buffer.append("; ").append(s.toString());
        }
        return buffer.toString();
    }
    
}