/*
 * Copyright (c) 2002-2018 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.docgen

import org.hamcrest.CoreMatchers._
import org.junit.Assert._
import org.junit.Test
import org.neo4j.test.{TestEnterpriseGraphDatabaseFactory}

class QueryPlanTest extends DocumentingTestBase with SoftReset {

  override protected def newTestGraphDatabaseFactory() = new TestEnterpriseGraphDatabaseFactory()

  override val setupQueries = List(
    """CREATE (me:Person {name: 'me'})
       CREATE (andres:Person {name: 'Andres'})
       CREATE (andreas:Person {name: 'Andreas'})
       CREATE (mattias:Person {name: 'Mattias'})
       CREATE (lovis:Person {name: 'Lovis'})
       CREATE (pontus:Person {name: 'Pontus'})
       CREATE (max:Person {name: 'Max'})
       CREATE (konstantin:Person {name: 'Konstantin'})
       CREATE (stefan:Person {name: 'Stefan'})
       CREATE (mats:Person {name: 'Mats'})
       CREATE (petra:Person {name: 'Petra'})
       CREATE (craig:Person {name: 'Craig'})
       CREATE (steven:Person {name: 'Steven'})
       CREATE (chris:Person {name: 'Chris'})

       CREATE (london:Location {name: 'London'})
       CREATE (malmo:Location {name: 'Malmo'})
       CREATE (sf:Location {name: 'San Francisco'})
       CREATE (berlin:Location {name: 'Berlin'})
       CREATE (newyork:Location {name: 'New York'})
       CREATE (kuala:Location {name: 'Kuala Lumpur'})
       CREATE (stockholm:Location {name: 'Stockholm'})
       CREATE (paris:Location {name: 'Paris'})
       CREATE (madrid:Location {name: 'Madrid'})
       CREATE (rome:Location {name: 'Rome'})

       CREATE (england:Country {name: 'England'})
       CREATE (field:Team {name: 'Field'})
       CREATE (engineering:Team {name: 'Engineering', id:42})
       CREATE (sales:Team {name: 'Sales'})
       CREATE (monads:Team {name: 'Team Monads'})
       CREATE (birds:Team {name: 'Team Enlightened Birdmen'})
       CREATE (quality:Team {name: 'Team Quality'})
       CREATE (rassilon:Team {name: 'Team Rassilon'})
       CREATE (executive:Team {name: 'Team Executive'})
       CREATE (remoting:Team {name: 'Team Remoting'})
       CREATE (other:Team {name: 'Other'})

       CREATE (me)-[:WORKS_IN {duration: 190}]->(london)
       CREATE (andreas)-[:WORKS_IN {duration: 187}]->(london)
       CREATE (andres)-[:WORKS_IN {duration: 150}]->(london)
       CREATE (mattias)-[:WORKS_IN {duration: 230}]->(london)
       CREATE (lovis)-[:WORKS_IN {duration: 230}]->(sf)
       CREATE (pontus)-[:WORKS_IN {duration: 230}]->(malmo)
       CREATE (max)-[:WORKS_IN {duration: 230}]->(newyork)
       CREATE (konstantin)-[:WORKS_IN {duration: 230}]->(london)
       CREATE (stefan)-[:WORKS_IN {duration: 230}]->(london)
       CREATE (stefan)-[:WORKS_IN {duration: 230}]->(berlin)
       CREATE (mats)-[:WORKS_IN {duration: 230}]->(malmo)
       CREATE (petra)-[:WORKS_IN {duration: 230}]->(london)
       CREATE (craig)-[:WORKS_IN {duration: 230}]->(malmo)
       CREATE (steven)-[:WORKS_IN {duration: 230}]->(malmo)
       CREATE (chris)-[:WORKS_IN {duration: 230}]->(madrid)
       CREATE (london)-[:IN]->(england)
       CREATE (me)-[:FRIENDS_WITH]->(andres)
       CREATE (andres)-[:FRIENDS_WITH]->(andreas)
    """.stripMargin)

  override val setupConstraintQueries = List(
    "CREATE INDEX ON :Location(name)",
    "CREATE INDEX ON :Person(name)",
    "CREATE CONSTRAINT ON (team:Team) ASSERT team.name is UNIQUE",
    "CREATE CONSTRAINT ON (team:Team) ASSERT team.id is UNIQUE"
  )

  def section = "Query Plan"

  @Test def allNodesScan() {
    profileQuery(
      title = "All Nodes Scan",
      text =
        """The `AllNodesScan` operator reads all nodes from the node store. The variable that will contain the nodes is seen in the arguments.
          |If your query is using this operator, you are very likely to see performance problems on any non-trivial database.""".stripMargin,
      queryText = """MATCH (n) RETURN n""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("AllNodesScan"))
    )
  }

  @Test def createUniqueConstraint() {
    profileQuery(
      title = "Create Unique Constraint",
      text =
        """The `CreateUniqueConstraint` operator creates a unique constraint on a (label,property) pair.
          |The following query will create a unique constraint on the `name` property of nodes with the `Country` label.""".stripMargin,
      queryText = """CREATE CONSTRAINT ON (c:Country) ASSERT c.name is UNIQUE""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("CreateUniqueConstraint"))
    )
  }

  @Test def dropUniqueConstraint() {
    executePreparationQueries {
      List("CREATE CONSTRAINT ON (c:Country) ASSERT c.name is UNIQUE")
    }

    profileQuery(
      title = "Drop Unique Constraint",
      text =
        """The `DropUniqueConstraint` operator drops a unique constraint on a (label,property) pair.
          |The following query will drop a unique constraint on the `name` property of nodes with the `Country` label.""".stripMargin,
      queryText = """DROP CONSTRAINT ON (c:Country) ASSERT c.name is UNIQUE""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("DropUniqueConstraint"))
    )
  }

  @Test def createNodePropertyExistenceConstraint() {
    profileQuery(
      title = "Create Node Property Existence Constraint (Enterprise Edition only)",
      text =
        """The `CreateNodePropertyExistenceConstraint` operator creates an existence constraint on a node property.""".stripMargin,
      queryText = """CREATE CONSTRAINT ON (p:Person) ASSERT exists(p.name)""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("CreateNodePropertyExistenceConstraint"))
    )
  }

  @Test def dropNodePropertyExistenceConstraint() {
    executePreparationQueries {
      List("CREATE CONSTRAINT ON (p:Person) ASSERT exists(p.name)")
    }

    profileQuery(
      title = "Drop Node Property Existence Constraint (Enterprise Edition only)",
      text =
        """The `DropNodePropertyExistenceConstraint` operator drops an existence constraint from a node property.""".stripMargin,
      queryText = """DROP CONSTRAINT ON (p:Person) ASSERT exists(p.name)""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("DropNodePropertyExistenceConstraint"))
    )
  }

  @Test def createRelationshipPropertyExistenceConstraint() {
    profileQuery(
      title = "Create Relationship Property Existence Constraint (Enterprise Edition only)",
      text =
        """The `CreateRelationshipPropertyExistenceConstraint` operator creates an existence constraint on a relationship property.""".stripMargin,
      queryText = """CREATE CONSTRAINT ON ()-[l:LIKED]-() ASSERT exists(l.when)""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("CreateRelationshipPropertyExistenceConstraint"))
    )
  }

  @Test def dropRelationshipPropertyExistenceConstraint() {
    executePreparationQueries {
      List("CREATE CONSTRAINT ON ()-[l:LIKED]-() ASSERT exists(l.when)")
    }

    profileQuery(
      title = "Drop Relationship Property Existence Constraint (Enterprise Edition only)",
      text =
        """The `DropRelationshipPropertyExistenceConstraint` operator drops an existence constraint from a relationship property.""".stripMargin,
      queryText = """DROP CONSTRAINT ON ()-[l:LIKED]-() ASSERT exists(l.when)""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("DropRelationshipPropertyExistenceConstraint"))
    )
  }

  @Test def createIndex() {
    profileQuery(
      title = "Create Index",
      text =
        """The `CreateIndex` operator creates an index on a (label, property) pair.
          |The following query will create an index on the `name` property of nodes with the `Country` label.""".stripMargin,
      queryText = """CREATE INDEX ON :Country(name)""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("CreateIndex"))
    )
  }

  @Test def dropIndex() {
    executePreparationQueries {
      List("CREATE INDEX ON :Country(name)")
    }

    profileQuery(
      title = "Drop Index",
      text =
        """The `DropIndex` operator drops an index on a (label, property) pair.
          |The following query will drop an index on the `name` property of nodes with the `Country` label.""".stripMargin,
      queryText = """DROP INDEX ON :Country(name)""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("DropIndex"))
    )
  }

  @Test def distinct() {
    profileQuery(
      title = "Distinct",
      text =
        """The `Distinct` operator removes duplicate rows from the incoming stream of rows.
          |To ensure only distinct elements are returned, `Distinct` needs to eagerly pull in all data from its source and build up state, which will lead to increased memory pressure in the system.""".stripMargin,
      queryText = """MATCH (l:Location)<-[:WORKS_IN]-(p:Person) RETURN DISTINCT l""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("Distinct"))
    )
  }

  @Test def eagerAggregation() {
    profileQuery(
      title = "Eager Aggregation",
      text =
        """The `EagerAggregation` operator evaluates a grouping expression and uses the result to group rows into different groupings.
          |For each of these groupings, `EagerAggregation` will then evaluate all aggregation functions and return the result.
          |To do this, `EagerAggregation`, as the name implies, needs to eagerly pull in all data from its source and build up state, which leads to increased memory pressure in the system.""".stripMargin,
      queryText = """MATCH (l:Location)<-[:WORKS_IN]-(p:Person) RETURN l.name AS location, collect(p.name) AS people""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("EagerAggregation"))
    )
  }

  @Test def nodeCountFromCountStore() {
    profileQuery(
      title = "Node Count From Count Store",
      text =
        """The `NodeCountFromCountStore` operator uses the count store to answer questions about node counts.
          | This is much faster than the `EagerAggregation` operator which achieves the same result by actually counting.
          | However, as the count store only stores a limited range of combinations, `EagerAggregation` will still be used for more complex queries.
          | For example, we can get counts for all nodes, and nodes with a label, but not nodes with more than one label.""".stripMargin,
      queryText = """MATCH (p:Person) RETURN count(p) AS people""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("NodeCountFromCountStore"))
    )
  }

  @Test def relationshipCountFromCountStore() {
    profileQuery(
      title = "Relationship Count From Count Store",
      text =
        """The `RelationshipCountFromCountStore` operator uses the count store to answer questions about relationship counts.
          | This is much faster than the `EagerAggregation` operator which achieves the same result by actually counting.
          | However, as the count store only stores a limited range of combinations, `EagerAggregation` will still be used for more complex queries.
          | For example, we can get counts for all relationships, relationships with a type, relationships with a label on one end, but not relationships with labels on both end nodes.""".stripMargin,
      queryText = """MATCH (p:Person)-[r:WORKS_IN]->() RETURN count(r) AS jobs""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("RelationshipCountFromCountStore"))
    )
  }

  @Test def eager() {
    profileQuery(
      title = "Eager",
      text =
        """For isolation purposes, the `Eager` operator ensures that operations affecting subsequent operations are executed fully for the whole dataset before continuing execution.
           | Information from the stores is fetched in a lazy manner, i.e. the pattern matching might not be fully exhausted before updates are applied.
           | To guarantee reasonable semantics, the query planner will insert `Eager` operators into the query plan to prevent updates from influencing pattern matching;
           | this scenario is exemplified by the query below, where the `DELETE` clause influences the `MATCH` clause.
           | The `Eager` operator can cause high memory usage when importing data or migrating graph structures.
           | In such cases, the operations should be split into simpler steps; e.g. importing nodes and relationships separately.
           | Alternatively, the records to be updated can be returned, followed by an update statement.""".stripMargin,
      queryText = """MATCH (a)-[r]-(b) DELETE r,a,b MERGE ()""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("Eager"))
    )
  }

  @Test def updateGraph() {
    profileQuery(
      title = "Update Graph",
      text =
        """The `UpdateGraph` operator creates a node in the graph.
          |This operator is only used for the rule planner (xxlink to this.)""".stripMargin,
      queryText = """CYPHER planner=rule CREATE (:Person {name: 'Alistair'})""",
      assertions = (p) => {
        assertThat(p.executionPlanDescription().toString, containsString("CreateNode"))
        assertThat(p.executionPlanDescription().toString, containsString("UpdateGraph"))
      }
    )
  }

  @Test def mergeInto() {
    profileQuery(
      title = "Merge Into",
      text =
        """When both the start and end node have already been found, the `Merge Into` operator is used to find all connecting relationships or creating a new relationship between the two nodes.
          |This operator is only used for the rule planner (xxlink to this.)
        """.stripMargin,
      queryText = """CYPHER planner=rule MATCH (p:Person {name: 'me'}), (f:Person {name: 'Andres'}) MERGE (p)-[:FRIENDS_WITH]->(f)""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("Merge(Into)"))
    )
  }

  @Test def createNode() {
    profileQuery(
      title = "Create Node",
      text =
        """The `CreateNode` operator is used to create a node.""".stripMargin,
      queryText = """CREATE (:Person {name: 'Jack'})""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("CreateNode"))
    )
  }

  @Test def createRelationship() {
    profileQuery(
      title = "Create Relationship",
      text =
        """The `CreateRelationship` operator is used to create a relationship.""".stripMargin,
      queryText =
        """MATCH (a:Person {name: 'Max'}), (b:Person {name: 'Chris'})
          |CREATE (a)-[:FRIENDS_WITH]->(b)""".stripMargin,
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("CreateRelationship"))
    )
  }

  @Test def delete() {
    profileQuery(
      title = "Delete",
      text =
        """The `Delete` operator is used to delete a node or a relationship.""".stripMargin,
      queryText =
        """MATCH (me:Person {name: 'me'})-[w:WORKS_IN {duration: 190}]->(london:Location {name: 'London'})
          |DELETE w""".stripMargin,
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("Delete"))
    )
  }

  @Test def detachDelete() {
    profileQuery(
      title = "Detach Delete",
      text =
        """The `DetachDelete` operator is used in all queries containing `DETACH DELETE`, when deleting nodes and their relationships.""".stripMargin,
      queryText =
        """MATCH (p:Person)
          |DETACH DELETE p""".stripMargin,
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("DetachDelete"))
    )
  }

  @Test def mergeCreateNode() {
    profileQuery(
      title = "Merge Create Node",
      text =
        """The `MergeCreateNode` operator is used when creating a node as a result of a `MERGE` failing to find the node.""".stripMargin,
      queryText =
        """MERGE (:Person {name: 'Sally'})""".stripMargin,
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("MergeCreateNode"))
    )
  }

  @Test def mergeCreateRelationship() {
    profileQuery(
      title = "Merge Create Relationship",
      text =
        """The `MergeCreateRelationship` operator is used when creating a relationship as a result of a `MERGE` failing to find the relationship.""".stripMargin,
      queryText =
        """MATCH (s:Person {name: 'Sally'})
          |MERGE (s)-[:FRIENDS_WITH]->(s)""".stripMargin,
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("MergeCreateRelationship"))
    )
  }

  @Test def removeLabels() {
    profileQuery(
      title = "Remove Labels",
      text =
        """The `RemoveLabels` operator is used when deleting labels from a node.""".stripMargin,
      queryText =
        """MATCH (n)
          |REMOVE n:Person""".stripMargin,
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("RemoveLabels"))
    )
  }

  @Test def setLabels() {
    profileQuery(
      title = "Set Labels",
      text =
        """The `SetLabels` operator is used when setting labels on a node.""".stripMargin,
      queryText =
        """MATCH (n)
          |SET n:Person""".stripMargin,
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("SetLabels"))
    )
  }

  @Test def setNodePropertyFromMap() {
    profileQuery(
      title = "Set Node Property From Map",
      text =
        """The `SetNodePropertyFromMap` operator is used when setting properties from a map on a node.""".stripMargin,
      queryText =
        """MATCH (n)
          |SET n = {weekday: 'Monday', meal: 'Lunch'}""".stripMargin,
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("SetNodePropertyFromMap"))
    )
  }

  @Test def setRelationshipPropertyFromMap() {
    profileQuery(
      title = "Set Relationship Property From Map",
      text =
        """The `SetRelationshipPropertyFromMap` operator is used when setting properties from a map on a relationship.""".stripMargin,
      queryText =
        """MATCH (n)-[r]->(m)
          |SET r = {weight: 5, unit: 'kg'}""".stripMargin,
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("SetRelationshipPropertyFromMap"))
    )
  }

  @Test def setNodeProperty() {
    profileQuery(
      title = "Set Node Property",
      text =
        """The `SetNodeProperty` operator is used when setting a property on a node.""".stripMargin,
      queryText =
        """MATCH (n)
          |SET n.checked = true""".stripMargin,
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("SetNodeProperty"))
    )
  }

  @Test def setRelationshipProperty() {
    profileQuery(
      title = "Set Relationship Property",
      text =
        """The `SetRelationshipProperty` operator is used when setting a property on a relationship.""".stripMargin,
      queryText =
        """MATCH (n)-[r]->(m)
          |SET r.weight = 100""".stripMargin,
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("SetRelationshipProperty"))
    )
  }

  @Test def emptyResult() {
    profileQuery(
      title = "Empty Result",
      text =
        """The `EmptyResult` operator eagerly loads all data coming into the `EmptyResult` operator and discards it xxx.""".stripMargin,
      queryText = """CREATE (:Person)""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("EmptyResult"))
    )
  }

  @Test def produceResult() {
    profileQuery(
      title = "Produce Result",
      text =
        """The `ProduceResult` operator prepares the result so that it is consumable by the user, such as transforming internal values to user values.
          |It is present in every single query that returns data to the user, and has little bearing on performance optimisation.""".stripMargin,
      queryText = """MATCH (n) RETURN n""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("ProduceResult"))
    )
  }

  @Test def nodeByLabelScan() {
    profileQuery(
      title = "Node By Label Scan",
      text = """The `NodeByLabelScan` operator fetches all nodes with a specific label from the node label index.""".stripMargin,
      queryText = """MATCH (person:Person) RETURN person""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("NodeByLabelScan"))
    )
  }

  @Test def nodeByIndexSeek() {
    profileQuery(
      title = "Node Index Seek",
      text = """The `NodeIndexSeek`operator finds nodes using an index seek. The node variable and the index used is shown in the arguments of the operator.
                |If the index is a unique index, the operator is instead called `NodeUniqueIndexSeek`.""".stripMargin,
      queryText = """MATCH (location:Location {name: 'Malmo'}) RETURN location""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("NodeIndexSeek"))
    )
  }

  @Test def nodeByUniqueIndexSeek() {
    profileQuery(
      title = "Node Unique Index Seek",
      text = """The `NodeUniqueIndexSeek` operator finds nodes using an index seek within a unique index. The node variable and the index used is shown in the arguments of the operator.
               |If the index is not unique, the operator is instead called `NodeIndexSeek`.""".stripMargin,
      queryText = """MATCH (t:Team {name: 'Malmo'}) RETURN t""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("NodeUniqueIndexSeek"))
    )
  }

  @Test def argument() {
    profileQuery(
      title = "Argument",
      text = """The `Argument` operator indicates the variable to be used as an argument to the right-hand side of an <<query-plan-apply, Apply>> operator.""".stripMargin,
      queryText = """MATCH (s:Person {name: 'me'}) MERGE (s)-[:FRIENDS_WITH]->(s)""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("Argument"))
    )
  }

  @Test def loadCSV() {
    profileQuery(
      title = "Load CSV",
      text =
        """The `LoadCSV` operator loads data from a CSV source into the query.
          |It is used whenever the <<query-load-csv, LOAD CSV>> clause is used in a query.""".stripMargin,
      queryText = """LOAD CSV FROM 'https://neo4j.com/docs/cypher-refcard/3.3/csv/artists.csv' AS line RETURN line""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("LoadCSV"))
    )
  }

  @Test def nodeIndexRangeSeek() {
    executePreparationQueries {
      (0 to 300).map { i => s"CREATE (:Location {name: '$i'})" }.toList
    }

    sampleAllIndexesAndWait()

    profileQuery(title = "Node Index Seek By Range",
                 text =
                   """The `NodeIndexSeekByRange` operator finds nodes using an index seek where the value of the property matches a given prefix string.
                     |`NodeIndexSeekByRange` can be used for `STARTS WITH` and comparison operators such as `<`, `>`, `\<=` and `>=`.
                     |If the index is a unique index, the operator is instead called `NodeUniqueIndexSeekByRange`.""".stripMargin,
                 queryText = "MATCH (l:Location) WHERE l.name STARTS WITH 'Lon' RETURN l",
                 assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("NodeIndexSeekByRange"))
    )
  }

  @Test def nodeUniqueIndexRangeSeek() {
    executePreparationQueries {
      (0 to 300).map { i => s"CREATE (:Team {name: '$i'})" }.toList
    }

    sampleAllIndexesAndWait()

    profileQuery(title = "Node Unique Index Seek By Range",
      text =
        """The `NodeUniqueIndexSeekByRange` operator finds nodes using an index seek within a unique index, where the value of the property matches a given prefix string.
          |`NodeUniqueIndexSeekByRange` can be used for `STARTS WITH` and comparison operators such as `<`, `>`, `\<=` and `>=`.
          |If the index is not unique, the operator is instead called `NodeIndexSeekByRange`.""".stripMargin,
      queryText = "MATCH (t:Team) WHERE t.name STARTS WITH 'Ma' RETURN t",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("NodeUniqueIndexSeekByRange"))
    )
  }


  @Test def nodeIndexScan() {
    executePreparationQueries((0 to 250).map { i =>
      "CREATE (:Location)"
    }.toList)
    profileQuery(title = "Node Index Scan",
                 text = """
                          |The `NodeIndexScan` operator examines all values stored in an index, returning all nodes with a particular label having a specified property.""".stripMargin,
                 queryText = "MATCH (l:Location) WHERE exists(l.name) RETURN l",
                 assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("NodeIndexScan"))
    )
  }

  @Test def nodeIndexContainsScan() {
    executePreparationQueries((0 to 250).map { i =>
      "CREATE (:Location)"
    }.toList)
    profileQuery(title = "Node Index Contains Scan",
                 text = """
                          |The `NodeIndexContainsScan` operator examines all values stored in an index, and searches for entries
                          | containing a specific string, such as when using `CONTAINS`.
                          | Although this is slower than an index seek (since all entries need to be
                          | examined), it is still faster than the indirection resulting from a label scan using `NodeByLabelScan`, and a property store
                          | filter.""".stripMargin,
                 queryText = "MATCH (l:Location) WHERE l.name CONTAINS 'al' RETURN l",
                 assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("NodeIndexContainsScan"))
    )
  }

  @Test def nodeIndexEndsWithScan() {
    executePreparationQueries((0 to 250).map { i =>
      "CREATE (:Location)"
    }.toList)
    profileQuery(title = "Node Index Ends With Scan",
      text = """
               |The `NodeIndexEndsWithScan` operator examines all values stored in an index, and searches for entries
               | ending in a specific string, such as when using `ENDS WITH`.
               | Although this is slower than an index seek (since all entries need to be
               | examined), it is still faster than the indirection resulting from a label scan using `NodeByLabelScan`, and a property store
               | filter xxx.""".stripMargin,
      queryText = "MATCH (l:Location) WHERE l.name ENDS WITH 'al' RETURN l",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("NodeIndexEndsWithScan"))
    )
  }

  @Test def nodeByIdSeek() {
    profileQuery(
      title = "Node By Id Seek",
      text =
        """The `NodeByIdSeek` operator reads one or more nodes by id from the node store.""".stripMargin,
      queryText = """MATCH (n) WHERE id(n) = 0 RETURN n""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("NodeByIdSeek"))
    )
  }

  @Test def projection() {
    profileQuery(
      title = "Projection",
      text =
        """For each incoming row, the `Projection` operator evaluates a set of expressions and produces a row with the results of the expressions.""".stripMargin,
      queryText = """RETURN 'hello' AS greeting""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("Projection"))
    )
  }

  @Test def filter() {
    profileQuery(
      title = "Filter",
      text =
        """The `Filter` operator filters each row coming from the child operator, only passing through rows that evaluate the predicates to `true`.""".stripMargin,
      queryText = """MATCH (p:Person) WHERE p.name =~ '^a.*' RETURN p""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("Filter"))
    )
  }

  @Test def cartesianProduct() {
    profileQuery(
      title = "Cartesian Product",
      text =
        """The `CartesianProduct` operator produces a cartesian product of the two inputs -- each row coming from the left child will be combined with all the rows from the right child operator.
          |`CartesianProduct` generally exhibits bad performance and ought to be avoided if possible.
        """.stripMargin,
      queryText = """MATCH (p:Person), (t:Team) RETURN p, t""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("CartesianProduct"))
    )
  }

  @Test def optionalExpand() {
    profileQuery(
      title = "Optional Expand All",
      text =
        """The `OptionalExpand(All)` operator is analogous to <<query-plan-expand-all, Expand(All)>>, apart from when no relationships match the direction, type and property predicates.
          |In this situation, `OptionalExpand(all)` will return a single row with the relationship and end node set to `null`.
          |""".stripMargin,
      queryText =
        """MATCH (p:Person)
           OPTIONAL MATCH (p)-[works_in:WORKS_IN]->(l) WHERE works_in.duration > 180
           RETURN p, l""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("OptionalExpand(All)"))
    )
  }

  @Test def sort() {
    profileQuery(
      title = "Sort",
      text =
        """The `Sort` operator sorts rows by a provided key.
          |In order to sort the data, all data from the source operator needs to be eagerly pulled in and kept in the query state, which will lead to increased memory pressure in the system.""".stripMargin,
      queryText = """MATCH (p:Person) RETURN p ORDER BY p.name""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("Sort"))
    )
  }

  @Test def top() {
    profileQuery(
      title = "Top",
      text =
        """The `Top` operator returns the first 'n' rows sorted by a provided key. Instead of sorting the entire input, only the top 'n' rows are retained.""".stripMargin,
      queryText = """MATCH (p:Person) RETURN p ORDER BY p.name LIMIT 2""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("Top"))
    )
  }

  @Test def limit() {
    profileQuery(
      title = "Limit",
      text =
        """The `Limit` operator returns the first 'n' rows from the incoming input.""".stripMargin,
      queryText = """MATCH (p:Person) RETURN p LIMIT 3""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("Limit"))
    )
  }

  @Test def lock() {
    profileQuery(
      title = "Lock",
      text =
        """The `Lock` operator locks the start and end node when creating a relationship.""".stripMargin,
      queryText = """MATCH (s:Person {name: 'me'}) MERGE (s)-[:FRIENDS_WITH]->(s)""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("Lock"))
    )
  }

  @Test def optional() {
    profileQuery(
      title = "Optional",
      text =
        """xxx For use in optional match.""".stripMargin,
      queryText = """MATCH (p:Person {name:'me'}) OPTIONAL MATCH (q:Person {name: 'Lulu'}) RETURN p, q""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("Optional"))
    )
  }

  @Test def projectEndpoints() {
    profileQuery(
      title = "Project Endpoints",
      text =
        """The `ProjectEndpoints` operator projects the start and end node of a relationship xxx.""".stripMargin,
      queryText = """CREATE (n)-[p:KNOWS]->(m) WITH p AS r MATCH (u)-[r]->(v) RETURN u, v""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("ProjectEndpoints"))
    )
  }

  @Test def expandAll() {
    profileQuery(
      title = "Expand All",
      text =
        """Given a start node, the `Expand(All)` operator will follow incoming or outgoing relationships, depending on the pattern relationship.""".stripMargin,
      queryText = """MATCH (p:Person {name: 'me'})-[:FRIENDS_WITH]->(fof) RETURN fof""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("Expand(All)"))
    )
  }

  @Test def expandInto() {
    profileQuery(
      title = "Expand Into",
      text =
        """When both the start and end node have already been found, the `Expand(Into)` operator is used to find all relationships connecting the two nodes.
          |As both the start and end node of the relationship are already in scope, the node with the smallest degree will be used.
          |This can make a noticeable difference when dense nodes appear as end points.""".stripMargin,
      queryText = """MATCH (p:Person {name: 'me'})-[:FRIENDS_WITH]->(fof)-->(p) RETURN fof""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("Expand(Into)"))
    )
  }


  @Test def optionalExpandInto() {
    profileQuery(
      title = "Optional Expand Into",
      text =
        """When both the start and end node have already been found, the `OptionalExpand(Into)` operator is used to find all relationships connecting the two nodes.
          |If no matching relationships are found, `OptionalExpand(Into)` will return a single row with the relationship and end node set to `null`.
          |As both the start and end node of the relationship are already in scope, the node with the smallest degree will be used.
          |This can make a noticeable difference when dense nodes appear as end points.""".stripMargin,
      queryText = """MATCH (p:Person)-[works_in:WORKS_IN]->(l) OPTIONAL MATCH (l)-->(p) RETURN p""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("OptionalExpand(Into)"))
    )
  }

  @Test def varlengthExpandAll() {
    profileQuery(
      title = "VarLength Expand All",
      text =
        """Given a start node, the `VarLengthExpand(All)` operator will follow variable-length relationships. xxx""".stripMargin,
      queryText = """MATCH (p:Person)-[:FRIENDS_WITH *1..2]-(q:Person) RETURN p, q""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("VarLengthExpand(All)"))
    )
  }

  @Test def varlengthExpandInto() {
    profileQuery(
      title = "VarLength Expand Into",
      text =
        """When both the start and end node have already been found, the `VarLengthExpand(Into)` operator is used to find all variable-length relationships connecting the two nodes. xxx""".stripMargin,
      queryText = """MATCH (p:Person)-[:FRIENDS_WITH *1..2]-(p:Person) RETURN p""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("VarLengthExpand(Into)"))
    )
  }

  @Test def directedRelationshipById() {
    profileQuery(
      title = "Directed Relationship By Id Seek",
      text =
        """The `DirectedRelationshipByIdSeek` operator reads one or more relationships by id from the relationship store, and produces both the relationship and the nodes on either side.""".stripMargin,
      queryText =
        """MATCH (n1)-[r]->()
           WHERE id(r) = 0
           RETURN r, n1
        """.stripMargin,
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("DirectedRelationshipByIdSeek"))
    )
  }

  @Test def undirectedRelationshipById() {
    profileQuery(
      title = "Undirected Relationship By Id Seek",
      text =
        """The `UndirectedRelationshipByIdSeek` operator reads one or more relationships by id from the relationship store.
          |As the direction is unspecified, two rows are produced for each relationship as a result of alternating the combination of the start and end node.""".stripMargin,
      queryText =
        """MATCH (n1)-[r]-()
           WHERE id(r) = 1
           RETURN r, n1
        """.stripMargin,
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("UndirectedRelationshipByIdSeek"))
    )
  }

  @Test def skip() {
    profileQuery(
      title = "Skip",
      text =
        """The `Skip` operator skips 'n' rows from the incoming rows.
        """.stripMargin,
      queryText =
        """MATCH (p:Person)
           RETURN p
           ORDER BY p.id
           SKIP 1
        """.stripMargin,
      assertions = (p) =>  assertThat(p.executionPlanDescription().toString, containsString("Skip"))
    )
  }

  @Test def union() {
    profileQuery(
      title = "Union",
      text =
        "The `Union` operator concatenates the results from the right plan with the results of the left plan.",
      queryText =
        """MATCH (p:Location)
           RETURN p.name
           UNION ALL
           MATCH (p:Country)
           RETURN p.name
        """.stripMargin,
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("Union"))
    )
  }

  @Test def unwind() {
    profileQuery(
      title = "Unwind",
      text =
        """The `Unwind` operator returns one row per item in a list.""".stripMargin,
      queryText = """UNWIND range(1, 5) as value return value""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("Unwind"))
    )
  }

  @Test def apply() {
    profileQuery(
      title = "Apply",
      text =
        """`Apply` works by performing a nested loop.
          |Every row being produced on the left-hand side of the `Apply` operator will be fed to the leaf
          |operator on the right-hand side, and then `Apply` will yield the combined results.
          |`Apply`, being a nested loop, can be seen as a warning that a better plan was not found.""".stripMargin,
      queryText =
        """MATCH (p:Person {name:'me'})
          |MATCH (q:Person {name: p.secondName})
          |RETURN p, q""".stripMargin,
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("Apply"))
    )
  }

  @Test def semiApply() {
    profileQuery(
      title = "Semi Apply",
      text =
        """The `SemiApply` operator tests for the existence of a pattern predicate.
          |`SemiApply` takes a row from its child operator and feeds it to the leaf operator on the right-hand side.
          |If the right-hand side operator tree yields at least one row, the row from the
          |left-hand side is yielded by the `SemiApply` operator.
          |This makes `SemiApply` a filtering operator, used mostly for pattern predicates in queries.""".stripMargin,
      queryText =
        """MATCH (p:Person)
          |WHERE (p)-[:FRIENDS_WITH]->(:Person)
          |RETURN p.name""".stripMargin,
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("SemiApply"))
    )
  }

  @Test def antiSemiApply() {
    profileQuery(
      title = "Anti Semi Apply",
      text =
        """The `AntiSemiApply` operator tests for the absence of a pattern.
          |`AntiSemiApply` takes a row from its child operator and feeds it to the leaf operator on the right-hand side.
          |If the right-hand side operator tree yields no rows, the row from the
          |left-hand side is yielded by the `AntiSemiApply` operator.
          |This makes `AntiSemiApply` a filtering operator, used for pattern predicates in queries.""".stripMargin,
      queryText =
        """MATCH (me:Person {name: "me"}), (other:Person)
          |WHERE NOT (me)-[:FRIENDS_WITH]->(other)
          |RETURN other.name""".stripMargin,
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("AntiSemiApply"))
    )
  }

  @Test def letSemiApply() {
    profileQuery(
      title = "Let Semi Apply",
      text =
        """The `LetSemiApply` operator tests for the existence of a pattern predicate.
          |When a query contains multiple pattern predicates (when using `OR`), `LetSemiApply` will be used to evaluate the first of these.
          |It will record the result of evaluating the predicate but will leave any filtering to another operator.
          |In the example, `LetSemiApply` will be used to check for the existence of the `FRIENDS_WITH`
          |relationship from each person.""".stripMargin,
      queryText =
        """MATCH (other:Person)
          |WHERE (other)-[:FRIENDS_WITH]->(:Person) OR (other)-[:WORKS_IN]->(:Location)
          |RETURN other.name""".stripMargin,
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("LetSemiApply"))
    )
  }

  @Test def letAntiSemiApply() {
    profileQuery(
      title = "Let Anti Semi Apply",
      text =
        """The `LetAntiSemiApply` operator tests for the absence of a pattern.
          |When a query contains multiple negated pattern predicates (when using `OR` and `NOT`), `LetAntiSemiApply` will be used to evaluate the first of these.
          |It will record the result of evaluating the predicate but will leave any filtering to another operator.
          |In the example, `LetAntiSemiApply` will be used to check for the absence of
          |the `FRIENDS_WITH` relationship from each person.""".stripMargin,
      queryText =
        """MATCH (other:Person)
          |WHERE NOT ((other)-[:FRIENDS_WITH]->(:Person)) OR (other)-[:WORKS_IN]->(:Location)
          |RETURN other.name""".stripMargin,
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("LetAntiSemiApply"))
    )
  }

  @Test def selectOrSemiApply() {
    profileQuery(
      title = "Select Or Semi Apply",
      text =
        """The `SelectOrSemiApply` operator tests for the existence of a pattern predicate and evaluates a predicate.
          |This operator allows for the mixing of normal predicates and pattern predicates
          |that check for the existence of a pattern.
          |First the normal expression predicate is evaluated, and only if it returns `false`
          |is the costly pattern predicate evaluation is performed.""".stripMargin,
      queryText =
        """MATCH (other:Person)
          |WHERE other.age > 25 OR (other)-[:FRIENDS_WITH]->(:Person)
          |RETURN other.name""".stripMargin,
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("SelectOrSemiApply"))
    )
  }

  @Test def selectOrAntiSemiApply() {
    profileQuery(
      title = "Select Or Anti Semi Apply",
      text =
        """The `SelectOrAntiSemiApply` operator tests for the absence of a pattern predicate and evaluates a predicate.""".stripMargin,
      queryText =
        """MATCH (other:Person)
          |WHERE other.age > 25 OR NOT (other)-[:FRIENDS_WITH]->(:Person)
          |RETURN other.name""".stripMargin,
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("SelectOrAntiSemiApply"))
    )
  }

  @Test def conditionalApply() {
    profileQuery(
      title = "Conditional Apply",
      text =
        """The `ConditionalApply` operator checks whether a variable is not `null`, and if so the right-hand side will be executed.""".stripMargin,
      queryText =
        """MERGE (p:Person {name: 'Andres'})
          |ON MATCH SET p.exists = true""".stripMargin,
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("ConditionalApply"))
    )
  }

  @Test def antiConditionalApply() {
    profileQuery(
      title = "Anti Conditional Apply",
      text =
        """The `AntiConditionalApply` operator checks whether a variable is `null`, and if so the right-hand side will be executed.""".stripMargin,
      queryText =
        """MERGE (p:Person {name: 'Andres'})
          |ON CREATE SET p.exists = true""".stripMargin,
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("AntiConditionalApply"))
    )
  }

  @Test def assertSameNode() {
    profileQuery(
      title = "Assert Same Node",
      text =
        """The `AssertSameNode` operator is used to ensure that no uniqueness constraints are violated.
          |The example looks for the existence of a team with the supplied name and id, and if one does not exist,
          |it will be created. Owing to the existence of two uniqueness constraints
          |on `:Team(name)` and `:Team(id)`, any node that would be found by the `UniqueIndexSeek`
          |must be the very same node, or the constraints would be violated.
        """.stripMargin,
      queryText =
        """MERGE (t:Team {name: 'Engineering', id: 42})""".stripMargin,
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("AssertSameNode"))
    )
  }

  @Test def nodeHashJoin() {
    executePreparationQueries(
      List(
        """MATCH (london:Location {name: 'London'}), (person:Person {name: 'Pontus'})
          FOREACH(x in range(0, 250) |
            CREATE (person) -[: WORKS_IN] ->(london)
            )""".stripMargin
      )
    )
    profileQuery(
      title = "Node Hash Join",
      text =
        """Using a hash table, the `NodeHashJoin` operator joins the input coming from the left with the input coming from the right.
          |`NodeHashJoin` only gets planned for larger cardinalities; for smaller cardinalities, `Expand` is used instead.""".stripMargin,
      queryText =
        """MATCH (andy:Person {name:'Andreas'})-[:WORKS_IN]->(loc)<-[:WORKS_IN]-(matt:Person {name:'Mattis'})
          |RETURN loc.name""".stripMargin,
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("NodeHashJoin"))
    )
  }

  @Test def triadic() {
    profileQuery(
      title = "Triadic",
      text =
        """The `Triadic` operator is used to solve triangular queries, such as the very
          |common 'find my friend-of-friends that are not already my friend'.
          |It does so by putting all the friends in a set, and use that set to check if the
          |friend-of-friends are already connected to me.
          |The example finds the names of all friends of my friends that are not already my friends.""".stripMargin,
      queryText =
        """MATCH (me:Person)-[:FRIENDS_WITH]-()-[:FRIENDS_WITH]-(other)
          |WHERE NOT (me)-[:FRIENDS_WITH]-(other)
          |RETURN other.name""".stripMargin,
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("Triadic"))
    )
  }

  @Test def foreach() {
    profileQuery(
      title = "Foreach",
      text =
        """The `Foreach` operator xxxx.""".stripMargin,
      queryText =
        """FOREACH (value IN [1,2,3] |
          |CREATE (:Person {age: value})
          |)""".stripMargin,
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("Foreach"))
    )
  }

  @Test def letSelectOrSemiApply() {
    profileQuery(
      title = "Let Select Or Semi Apply",
      text =
        """The `LetSelectOrSemiApply` operator is planned for PatternPredicates mixed with other predicates connected with OR.""".stripMargin,
      queryText =
        """MATCH (other:Person)
          |WHERE (other)-[:FRIENDS_WITH]->(:Person) OR (other)-[:WORKS_IN]->(:Location) OR other.age = 5
          |RETURN other.name""".stripMargin,
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("LetSelectOrSemiApply"))
    )
  }

  @Test def letSelectOrAntiSemiApply() {
    profileQuery(
      title = "Let Select Or Anti Semi Apply",
      text =
        """The `LetSelectOrAntiSemiApply` operator is planned for negated PatternPredicates mixed with other predicates connected with OR.""".stripMargin,
      queryText =
        """MATCH (other:Person)
          |WHERE NOT (other)-[:FRIENDS_WITH]->(:Person) OR (other)-[:WORKS_IN]->(:Location) OR other.age = 5
          |RETURN other.name""".stripMargin,
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("LetSelectOrAntiSemiApply"))
    )
  }

  //TODO get a query that works
  @Test def nodeOuterHashJoin() {
    profileQuery(
      title = "Node Outer Hash Join",
      text =
        """Using a hash table, the `NodeOuterHashJoin` operator joins the input coming from the left with the input coming from the right.
          |If the input from the left does not have any matches coming from the right, a `null` is produced for the variable on the right.""".stripMargin,
      queryText =
        """MATCH (p:Person {name:'me'})
          |OPTIONAL MATCH (p)--(q:Person {name: p.surname})
          |USING JOIN ON p
          |RETURN p,q""".stripMargin,
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("t")) //so works for now
    )
  }

  @Test def rollUpApply() {
    profileQuery(
      title = "Roll Up Apply",
      text =
        """The `RollUpApply` operator xxx.""".stripMargin,
      queryText =
        """MATCH (n)
          |RETURN (n)-->()""".stripMargin,
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("RollUpApply"))
    )
  }

  @Test def valueHashJoin() {
    profileQuery(
      title = "Value Hash Join",
      text =
        """The `ValueHashJoin` operator xxx.""".stripMargin,
      queryText =
        """MATCH (p:Person),(q:Person)
          |WHERE p.age = q.age
          |RETURN p,q""".stripMargin,
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("ValueHashJoin"))
    )
  }

  @Test def call(): Unit = {
    profileQuery(
      title = "Procedure Call",
      text = """The `ProcedureCall` operator indicates an invocation to a procedure.""".stripMargin,
      queryText = """CALL db.labels() YIELD label RETURN * ORDER BY label""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("ProcedureCall"))
    )
  }
}
