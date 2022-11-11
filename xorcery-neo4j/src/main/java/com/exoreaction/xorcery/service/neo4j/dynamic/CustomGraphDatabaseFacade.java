package com.exoreaction.xorcery.service.neo4j.dynamic;

import org.neo4j.configuration.Config;
import org.neo4j.dbms.systemgraph.TopologyGraphDbmsModel;
import org.neo4j.internal.kernel.api.connectioninfo.ClientConnectionInfo;
import org.neo4j.internal.kernel.api.exceptions.TransactionFailureException;
import org.neo4j.internal.kernel.api.security.LoginContext;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.kernel.api.exceptions.Status;
import org.neo4j.kernel.availability.DatabaseAvailabilityGuard;
import org.neo4j.kernel.availability.UnavailableException;
import org.neo4j.kernel.database.Database;
import org.neo4j.kernel.impl.api.CloseableResourceManager;
import org.neo4j.kernel.impl.coreapi.InternalTransaction;
import org.neo4j.kernel.impl.coreapi.TransactionExceptionMapper;
import org.neo4j.kernel.impl.coreapi.TransactionImpl;
import org.neo4j.kernel.impl.factory.DbmsInfo;
import org.neo4j.kernel.impl.factory.GraphDatabaseFacade;

import java.util.function.Consumer;
import java.util.function.Function;

public class CustomGraphDatabaseFacade
    extends GraphDatabaseFacade
{
    private Database database;
    private DatabaseAvailabilityGuard availabilityGuard;
    private Function<LoginContext, LoginContext> loginContextTransformer;

    public CustomGraphDatabaseFacade(Database database, Config config, DbmsInfo dbmsInfo, DatabaseAvailabilityGuard availabilityGuard) {
        super(database, config, dbmsInfo, TopologyGraphDbmsModel.HostedOnMode.SINGLE, availabilityGuard);
        this.database = database;
        this.availabilityGuard = availabilityGuard;
        this.loginContextTransformer = Function.identity();
    }

    @Override
    protected InternalTransaction beginTransactionInternal(KernelTransaction.Type type, LoginContext loginContext, ClientConnectionInfo connectionInfo, long timeoutMillis, Consumer<Status> terminationCallback, TransactionExceptionMapper transactionExceptionMapper) {

        KernelTransaction kernelTransaction = this.beginKernelTransaction(type, loginContext, connectionInfo, timeoutMillis);
        return new CustomTransactionImpl(this.database.getTokenHolders(), this.contextFactory, this.availabilityGuard, this.database.getExecutionEngine(), kernelTransaction, new CloseableResourceManager(), terminationCallback, transactionExceptionMapper, this.database.getElementIdMapper());
    }

    KernelTransaction beginKernelTransaction(KernelTransaction.Type type, LoginContext loginContext, ClientConnectionInfo connectionInfo, long timeout) {
        try {
            this.availabilityGuard.assertDatabaseAvailable();
            return this.database.getKernel().beginTransaction(type, (LoginContext)this.loginContextTransformer.apply(loginContext), connectionInfo, timeout);
        } catch (TransactionFailureException | UnavailableException var7) {
            throw new org.neo4j.graphdb.TransactionFailureException(var7.getMessage(), var7);
        }
    }

}
