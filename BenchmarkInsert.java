package com.vaticle.typedb.benchmark;

import com.vaticle.typedb.client.TypeDB;
import com.vaticle.typedb.client.api.TypeDBClient;
import com.vaticle.typedb.client.api.TypeDBCredential;
import com.vaticle.typedb.client.api.TypeDBSession;
import com.vaticle.typedb.client.api.TypeDBTransaction;
import com.vaticle.typedb.client.api.answer.ConceptMap;
import com.vaticle.typedb.client.common.exception.TypeDBClientException;
import com.vaticle.typeql.lang.TypeQL;
import com.vaticle.typeql.lang.common.TypeQLArg;
import com.vaticle.typeql.lang.query.TypeQLDefine;
import com.vaticle.typeql.lang.query.TypeQLInsert;
import com.vaticle.typeql.lang.query.TypeQLMatch;
import com.vaticle.typeql.lang.query.TypeQLQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.vaticle.typedb.common.collection.Collections.set;
import static com.vaticle.typeql.lang.TypeQL.define;
import static com.vaticle.typeql.lang.TypeQL.insert;
import static com.vaticle.typeql.lang.TypeQL.match;
import static com.vaticle.typeql.lang.TypeQL.type;
import static com.vaticle.typeql.lang.TypeQL.var;
import static org.junit.Assert.assertEquals;


public class BenchmarkInsert {
    
    public static void main(String[] args) {
        if (args.length != 3) {
            throw new RuntimeException("Product, address and count must be specified");
        }
        boolean isCore = args[0].equals("typedb");

        String address = args[1];
        int count;
        try {
            count = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            throw new RuntimeException("count must be a number");
        }

        String db = "benchmark-insert";
        System.out.println("Creating database " + db  + "...");
        try (TypeDBClient client = createClient(isCore, address)) {
            if (client.databases().contains(db)) client.databases().get(db).delete();
            client.databases().create(db);
        }
        System.out.println("Performing 'insertion benchmark' with " + count + " instances...");
        long start = System.currentTimeMillis();
        try (TypeDBClient client = createClient(isCore, address)) {
            Util.insertPersonType(db, client);
            Util.assertPersonType(db, client);
            Util.insertPerson(db, count, client);
            Util.assertPerson(db, count, client);
        }
        System.out.println("'Insertion benchmark' finished in " + (System.currentTimeMillis() - start)  + "ms");
    }

    private static TypeDBClient createClient(boolean isCore, String address) {
        if (isCore) {
            System.out.println("Creating Core client...");
            return TypeDB.coreClient(address);
        } else {
            System.out.println("Creating Cluster client...");
            return TypeDB.clusterClient(address, new TypeDBCredential("admin", "password", false));
        }
    }

    public static class Util {

        private static final Logger LOG = LoggerFactory.getLogger(Util.class);

        public static void insertPersonType(String database, TypeDBClient client) {
            try (TypeDBSession session = client.session(database, TypeDBSession.Type.SCHEMA);
                 TypeDBTransaction tx = session.transaction(TypeDBTransaction.Type.WRITE)) {

                TypeQLDefine query = define(
                        type("person").sub("entity"),
                        type("username").sub("attribute").value(TypeQLArg.ValueType.STRING),
                        type("password").sub("attribute").value(TypeQLArg.ValueType.STRING),
                        type("person").owns("username").owns("password")
                );
                tx.query().define(query);
                tx.commit();
            }
        }

        public static void assertPersonType(String database, TypeDBClient client) {
            try (TypeDBSession session = client.session(database, TypeDBSession.Type.SCHEMA);
                 TypeDBTransaction tx = session.transaction(TypeDBTransaction.Type.READ)) {
                TypeQLQuery query = TypeQL.parseQuery("match $x sub person;");
                List<ConceptMap> types = tx.query().match(query.asMatch()).collect(Collectors.toList());
                assertEquals(1, types.size());
            }
        }

        public static int insertPerson(String database, int count, TypeDBClient client) {
            return insertPerson(database, 0, count, client);
        }

        public static int insertPerson(String database, int start, int count, TypeDBClient client) {
            int inserted = 0;
            for (int i = start; i < (count+start); ++i) {
                try (TypeDBSession session = client.session(database, TypeDBSession.Type.DATA);
                     TypeDBTransaction tx = session.transaction(TypeDBTransaction.Type.WRITE)) {
                    TypeQLInsert query = insert(
                            var("p").isa("person")
                                    .has("username", "username" + i)
                                    .has("password", "password" + i)
                    );
                    tx.query().insert(query);
                    tx.commit();
                    inserted++;
                } catch (TypeDBClientException e) {
                    LOG.error("An error has occurred during the insertion of a person instance", e);
                }
            }
            return inserted;
        }

        public static void assertPerson(String database, int count, TypeDBClient client) {
            assertPerson(database, count, set(), client);
        }

        public static void assertPerson(String database, int count, Set<Integer> exclude, TypeDBClient client) {
            for (int i = 0; i < count; ++i) {
                if (exclude.contains(i)) {
                    System.out.println("test");
                } else {
                    try (TypeDBSession session = client.session(database, TypeDBSession.Type.DATA);
                         TypeDBTransaction tx = session.transaction(TypeDBTransaction.Type.WRITE)) {
                        TypeQLMatch query = match(
                                var("p").isa("person")
                                        .has("username", "username" + i)
                                        .has("password", "password" + i)
                        );
                        List<ConceptMap> answers = tx.query().match(query).collect(Collectors.toList());
                        assertEquals(1, answers.size());
                    }
                }
            }
        }
    }

}
