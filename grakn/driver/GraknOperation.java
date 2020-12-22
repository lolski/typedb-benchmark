/*
 * Copyright (C) 2020 Grakn Labs
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package grakn.simulation.grakn.driver;

import grakn.client.GraknClient;
import grakn.client.answer.ConceptMap;
import grakn.simulation.common.driver.LogWrapper;
import grakn.simulation.common.driver.TransactionalDbOperation;
import graql.lang.query.GraqlDelete;
import graql.lang.query.GraqlGet;
import graql.lang.query.GraqlInsert;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Iterables.getOnlyElement;

public class GraknOperation extends TransactionalDbOperation {

    private final GraknClient.Transaction transaction;
    private final LogWrapper log;

    boolean closed = false;

    public GraknOperation(GraknClient.Session session, LogWrapper log, String tracker, boolean trace) {
        super(tracker, trace);
        this.transaction = session.transaction(GraknClient.Transaction.Type.WRITE);
        this.log = log;
    }

    @Override
    public void close() {
        transaction.close();
        closed = true;
    }

    @Override
    public void save() {
        throwIfClosed();
        transaction.commit();
        closed = true;
    }

    private void throwIfClosed() {
        if (closed) {
            throw new RuntimeException("Transaction is closed, please open a new one.");
        }
    }

    public <T> List<T> sortedExecute(GraqlGet query, String attributeName, Integer limit){
        throwIfClosed();
        log.query(tracker, query);
        Stream<T> answerStream = transaction.stream(query)
                .map(conceptMap -> (T) conceptMap.get(attributeName).asAttribute().value())
                .sorted();
        if (limit != null) {
            answerStream = answerStream.limit(limit);
        }
        return answerStream.collect(Collectors.toList());
    }

    public void execute(GraqlDelete query) {
        log.query(tracker, query);
        transaction.execute(query);
    }

    public List<ConceptMap> execute(GraqlInsert query) {
        log.query(tracker, query);
        return transaction.execute(query);
    }

    public List<ConceptMap> execute(GraqlGet query) {
        log.query(tracker, query);
        return transaction.execute(query);
    }

    public Number execute(GraqlGet.Aggregate query) {
        log.query(tracker, query);
        return getOnlyElement(transaction.execute(query)).number();
    }

    public Object getOnlyAttributeOfThing(ConceptMap answer, String varName, String attributeType) {
        return getOnlyElement(answer.get(varName).asThing().asRemote(transaction).attributes(transaction.getAttributeType(attributeType)).collect(Collectors.toList())).value();
    }

    public Object getValueOfAttribute(ConceptMap answer, String varName) {
        return answer.get(varName).asAttribute().value();
    }
}
