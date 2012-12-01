/*Copyright 2009 Enrico Nicoletti
 eMail: enrico.nicoletti84@gmail.com

 This file is part of Freedomotic.

 Freedomotic is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 any later version.

 Freedomotic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with EventEngine; if not, write to the Free Software
 Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package it.freedomotic.bus;

import it.freedomotic.app.Freedomotic;
import it.freedomotic.util.Info;
import java.net.BindException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;

/**
 *
 * @author enrico
 */
public class AbstractBusConnector {

    protected static final String DEFAULT_USER = "user";
    protected static final String DEFAULT_PASSWORD = "password";
    protected Connection connection;
    protected MessageConsumer receiver;
    protected MessageProducer sender;
    private static Session sharedSession;
    private static Session unlistenedSession;
    private static MessageProducer emptySharedWriter;
    private static final BrokerService BROKER = new BrokerService();

    public AbstractBusConnector() {

        connect(Info.BROKER_DEFAULT, DEFAULT_USER, DEFAULT_PASSWORD);
    }

    private void connect(String brokerString, String username, String password) {
        //create a connection
        if (emptySharedWriter == null) { //not already connected to the bus
            try {
                //create an embedded messaging broker
                //BROKER.setBrokerName("freedomotic");
                //use always 0.0.0.0 not localhost. localhost allows connections 
                //only on the local machine not from LAN IPs
                try {
                    BROKER.addConnector(Info.BROKER_STOMP);
//                //websocket connector for javascript apps
                    BROKER.addConnector(Info.BROKER_WEBSOCKET);
                } catch (Exception exception) {
                    Freedomotic.logger.warning("Broker connector not started due to " + exception.getLocalizedMessage());
                }
                BROKER.setPersistent(false); //we don't need to save messages on disk
                BROKER.setUseJmx(false);
                //start the broker
                BROKER.start();

                //connect to the embedded broker defined above
                ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(Info.BROKER_DEFAULT);
                //tuned for performances http://activemq.apache.org/performance-tuning.html
                factory.setUseAsyncSend(true);
                factory.setOptimizeAcknowledge(true);
                factory.setAlwaysSessionAsync(true);
                factory.setObjectMessageSerializationDefered(true);
                factory.setCopyMessageOnSend(false);

                connection = factory.createConnection(username, password);
                sharedSession = connection.createSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
                unlistenedSession = connection.createSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE); //an unlistened session
                emptySharedWriter = unlistenedSession.createProducer(null); //a shared bus writer for all freedomotic classes
                emptySharedWriter.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
                new StompDispatcher(); //just for testing, don't mind it
                connection.start();
            } catch (JMSException jMSException) {
                Freedomotic.logger.severe(Freedomotic.getStackTraceInfo(jMSException));
            } catch (Exception ex) {
                Logger.getLogger(AbstractBusConnector.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    protected Session getBusUnlistenedSession() {
        return unlistenedSession;
    }

    protected Session getBusSharedSession() {
        return sharedSession;
    }

    protected MessageProducer getBusSharedWriter() {
        return emptySharedWriter;
    }

    public static void disconnect() {
        try {
            BROKER.stop();
        } catch (Exception ex) {
            Logger.getLogger(AbstractBusConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
