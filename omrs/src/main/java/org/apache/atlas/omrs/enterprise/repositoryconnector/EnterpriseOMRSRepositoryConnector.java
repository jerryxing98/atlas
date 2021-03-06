/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.omrs.enterprise.repositoryconnector;

import org.apache.atlas.ocf.ffdc.ConnectorCheckedException;
import org.apache.atlas.omrs.enterprise.connectormanager.OMRSConnectorConsumer;
import org.apache.atlas.omrs.enterprise.connectormanager.OMRSConnectorManager;
import org.apache.atlas.omrs.ffdc.exception.RepositoryErrorException;
import org.apache.atlas.omrs.metadatacollection.OMRSMetadataCollection;
import org.apache.atlas.omrs.metadatacollection.repositoryconnector.OMRSRepositoryConnector;
import org.apache.atlas.omrs.ffdc.OMRSErrorCode;
import org.apache.atlas.omrs.ffdc.exception.OMRSRuntimeException;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * EnterpriseOMRSRepositoryConnector supports federating calls to multiple metadata repositories.  As a result,
 * its OMRSMetadataCollection (EnterpriseOMRSMetadataCollection) returns metadata from all repositories in the
 * connected open metadata repository cohort(s).
 * <p>
 *     An instance of the EnterpriseOMRSRepositoryConnector is created by each Open Metadata Access Service (OMAS)
 *     using the OCF ConnectorBroker.  They use its metadata collection to retrieve and send the metadata they need.
 * </p>
 * <p>
 *     Each EnterpriseOMRSRepositoryConnector instance needs to maintain an up to date list of OMRS Connectors to all of the
 *     repositories in the connected open metadata repository cohort(s).  It does by registering as an OMRSConnectorConsumer
 *     with the OMRSConnectorManager to be notified when connectors to new open metadata repositories are available.
 * </p>
 */
public class EnterpriseOMRSRepositoryConnector extends OMRSRepositoryConnector implements OMRSConnectorConsumer
{
    private OMRSConnectorManager             connectorManager                 = null;
    private String                           connectorConsumerId              = null;

    private EnterpriseOMRSMetadataCollection enterpriseMetadataCollection     = null;
    private String                           enterpriseMetadataCollectionId   = null;
    private String                           enterpriseMetadataCollectionName = null;

    private boolean                          connected                        = false;

    private FederatedConnector               localCohortConnector             = null;
    private ArrayList<FederatedConnector>    remoteCohortConnectors           = new ArrayList<>();


    /**
     * Constructor used by the EnterpriseOMRSConnectorProvider.
     *
     * @param connectorManager - provides notifications as repositories register and unregister with the
     *                         cohorts.
     * @param enterpriseMetadataCollectionName - name of the virtual metadata collection that this
     *                                         connector is managing.
     */
    public EnterpriseOMRSRepositoryConnector(OMRSConnectorManager connectorManager,
                                             String               enterpriseMetadataCollectionName)
    {
        super();

        this.enterpriseMetadataCollectionName = enterpriseMetadataCollectionName;

        this.connectorManager = connectorManager;

        if (connectorManager != null)
        {
            this.connectorConsumerId = connectorManager.registerConnectorConsumer(this);
        }
        else
        {
            OMRSErrorCode errorCode = OMRSErrorCode.INVALID_COHORT_CONFIG;
            String errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage();

            throw new OMRSRuntimeException(errorCode.getHTTPErrorCode(),
                                           this.getClass().getName(),
                                           "constructor",
                                           errorMessage,
                                           errorCode.getSystemAction(),
                                           errorCode.getUserAction());
        }
    }


    /**
     * Set up the unique Id for this metadata collection.
     *
     * @param metadataCollectionId - String unique Id
     */
    public void setMetadataCollectionId(String     metadataCollectionId)
    {
        this.enterpriseMetadataCollectionId = metadataCollectionId;

        if (metadataCollectionId != null)
        {
            enterpriseMetadataCollection = new EnterpriseOMRSMetadataCollection(this,
                                                                                enterpriseMetadataCollectionId,
                                                                                enterpriseMetadataCollectionName);

            connected = true;
        }
    }


    /**
     * Returns the metadata collection object that provides an OMRS abstraction of the metadata within
     * a metadata repository.  For the EnterpriseOMRSRepositoryConnector, this is the metadata collection that is
     * configured to work across the cohort.
     *
     * @return OMRSMetadataCollection - metadata information retrieved from the metadata repository.
     */
    public OMRSMetadataCollection getMetadataCollection()
    {
        String   methodName = "getMetadataCollection()";

        if (enterpriseMetadataCollection == null)
        {
            OMRSErrorCode errorCode = OMRSErrorCode.INVALID_COHORT_CONFIG;
            String errorMessage = errorCode.getErrorMessageId()
                                + errorCode.getFormattedErrorMessage();

            throw new OMRSRuntimeException(errorCode.getHTTPErrorCode(),
                                           this.getClass().getName(),
                                           methodName,
                                           errorMessage,
                                           errorCode.getSystemAction(),
                                           errorCode.getUserAction());
        }

        return enterpriseMetadataCollection;
    }

    /**
     * Free up any resources held since the connector is no longer needed.
     *
     * @throws ConnectorCheckedException - there is a problem disconnecting the connector.
     */
    public void disconnect() throws ConnectorCheckedException
    {
        if ((connectorManager != null) && (connectorConsumerId != null))
        {
            connectorManager.unregisterConnectorConsumer(connectorConsumerId);
        }

        localCohortConnector = null;
        remoteCohortConnectors = new ArrayList<>();
        enterpriseMetadataCollection = null;
        connected = false;
    }


    /**
     * Returns the list of metadata collections that the EnterpriseOMRSRepositoryConnector is federating queries across.
     *
     * This method is used by this connector's metadata collection object on each request it processes.  This
     * means it always has the most up to date list of metadata collections to work with.
     *
     * @return OMRSMetadataCollection ArrayList
     * @throws RepositoryErrorException - the enterprise services are not available
     */
    protected  ArrayList<OMRSMetadataCollection>   getMetadataCollections() throws RepositoryErrorException
    {
        String   methodName = "getMetadataCollections()";

        if (connected)
        {
            ArrayList<OMRSMetadataCollection> cohortMetadataCollections = new ArrayList<>();

            /*
             * Make sure the local connector is first.
             */
            if (localCohortConnector != null)
            {
                cohortMetadataCollections.add(localCohortConnector.getMetadataCollection());
            }

            /*
             * Now add the remote connectors.
             */
            for (FederatedConnector federatedConnector : remoteCohortConnectors)
            {
                cohortMetadataCollections.add(federatedConnector.getMetadataCollection());
            }

            if (cohortMetadataCollections.size() > 0)
            {
                return cohortMetadataCollections;
            }
            else
            {
                OMRSErrorCode errorCode = OMRSErrorCode.NO_REPOSITORIES;
                String errorMessage = errorCode.getErrorMessageId()
                                    + errorCode.getFormattedErrorMessage();

                throw new RepositoryErrorException(errorCode.getHTTPErrorCode(),
                                                   this.getClass().getName(),
                                                   methodName,
                                                   errorMessage,
                                                   errorCode.getSystemAction(),
                                                   errorCode.getUserAction());
            }
        }
        else
        {
            OMRSErrorCode errorCode = OMRSErrorCode.ENTERPRISE_DISCONNECTED;
            String errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage();

            throw new RepositoryErrorException(errorCode.getHTTPErrorCode(),
                                           this.getClass().getName(),
                                           methodName,
                                           errorMessage,
                                           errorCode.getSystemAction(),
                                           errorCode.getUserAction());
        }
    }


    /**
     * Save the connector to the local repository.  This is passed from the OMRSConnectorManager.
     *
     * @param metadataCollectionId - Unique identifier for the metadata collection
     * @param localConnector - OMRSRepositoryConnector object for the local repository.
     */
    public void setLocalConnector(String                  metadataCollectionId,
                                  OMRSRepositoryConnector localConnector)
    {
        if (localConnector != null)
        {
            localCohortConnector = new FederatedConnector(metadataCollectionId,
                                                           localConnector,
                                                           localConnector.getMetadataCollection());
        }
        else
        {
            localCohortConnector = null;
        }
    }


    /**
     * Pass the connector to one of the remote repositories in the metadata repository cohort.
     *
     * @param metadataCollectionId - Unique identifier for the metadata collection
     * @param remoteConnector - OMRSRepositoryConnector object providing access to the remote repository.
     */
    public void addRemoteConnector(String                  metadataCollectionId,
                                   OMRSRepositoryConnector remoteConnector)
    {
        if (remoteConnector != null)
        {
            remoteCohortConnectors.add(new FederatedConnector(metadataCollectionId,
                                                               remoteConnector,
                                                               remoteConnector.getMetadataCollection()));
        }
    }


    /**
     * Pass the metadata collection id for a repository that has just left the metadata repository cohort.
     *
     * @param metadataCollectionId - identifier of the metadata collection that is no longer available.
     */
    public void removeRemoteConnector(String  metadataCollectionId)
    {
        Iterator<FederatedConnector> iterator = remoteCohortConnectors.iterator();

        while(iterator.hasNext())
        {
            FederatedConnector registeredConnector = iterator.next();

            if (registeredConnector.getMetadataCollectionId().equals(metadataCollectionId))
            {
                iterator.remove();
            }
        }
    }

    /**
     * Call disconnect on all registered connectors and stop calling them.  The OMRS is about to shutdown.
     */
    public void disconnectAllConnectors()
    {
        // TODO
    }


    /**
     * FederatedConnector is a private class for storing details of each of the connectors to the repositories
     * in the open metadata repository cohort.
     */
    private class FederatedConnector
    {
        private String                  metadataCollectionId = null;
        private OMRSRepositoryConnector connector            = null;
        private OMRSMetadataCollection  metadataCollection   = null;


        /**
         * Constructor to set up the details of a federated connector.
         *
         * @param metadataCollectionId - unique identifier for the metadata collection accessed through the connector
         * @param connector - connector for the repository
         * @param metadataCollection - metadata collection from the connection.
         */
        public FederatedConnector(String metadataCollectionId, OMRSRepositoryConnector connector, OMRSMetadataCollection metadataCollection)
        {
            this.metadataCollectionId = metadataCollectionId;
            this.connector = connector;
            this.metadataCollection = metadataCollection;
        }


        /**
         * Return the identifier for the metadata collection accessed through the connector.
         *
         * @return String identifier
         */
        public String getMetadataCollectionId()
        {
            return metadataCollectionId;
        }


        /**
         * Return the connector for the repository.
         *
         * @return OMRSRepositoryConnector object
         */
        public OMRSRepositoryConnector getConnector()
        {
            return connector;
        }


        /**
         * Return the metadata collection associated with the connector.
         *
         * @return OMRSMetadataCollection object
         */
        public OMRSMetadataCollection getMetadataCollection()
        {
            return metadataCollection;
        }
    }
}
