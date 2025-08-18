/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.nativetests;

import com.facebook.presto.testing.QueryRunner;
import com.facebook.presto.tests.AbstractTestQueryFramework;
import org.testng.annotations.Test;

import static com.facebook.presto.nativetests.NativeTestsUtils.createCollection;
import static com.facebook.presto.nativetests.NativeTestsUtils.createCollections;
import static com.facebook.presto.nativetests.NativeTestsUtils.createCounties;
import static com.facebook.presto.nativetests.NativeTestsUtils.createCurves;
import static com.facebook.presto.nativetests.NativeTestsUtils.createDepartment;
import static com.facebook.presto.nativetests.NativeTestsUtils.createDrainage;
import static com.facebook.presto.nativetests.NativeTestsUtils.createEveryplace;
import static com.facebook.presto.nativetests.NativeTestsUtils.createGeometries;
import static com.facebook.presto.nativetests.NativeTestsUtils.createGeometryTest;
import static com.facebook.presto.nativetests.NativeTestsUtils.createLines;
import static com.facebook.presto.nativetests.NativeTestsUtils.createMulticurves;
import static com.facebook.presto.nativetests.NativeTestsUtils.createMultilines;
import static com.facebook.presto.nativetests.NativeTestsUtils.createMultipoints;
import static com.facebook.presto.nativetests.NativeTestsUtils.createMultipolygons;
import static com.facebook.presto.nativetests.NativeTestsUtils.createMultisurfaces;
import static com.facebook.presto.nativetests.NativeTestsUtils.createPlaces;
import static com.facebook.presto.nativetests.NativeTestsUtils.createPoints;
import static com.facebook.presto.nativetests.NativeTestsUtils.createPolygons;
import static com.facebook.presto.nativetests.NativeTestsUtils.createRoads;
import static com.facebook.presto.nativetests.NativeTestsUtils.createSaleszones;
import static com.facebook.presto.nativetests.NativeTestsUtils.createSurfaces;
import static org.testng.Assert.assertEquals;

public abstract class AbstractTestNativeGeoFunctions
        extends AbstractTestQueryFramework
{
    // NOTE: Can confirm output with db2/test/fvt/standalone/gse_relations/exp/st_*.rxp
    // for any of the tests below. For any table with more than x and y coordinates, the z
    // coordinate and measure is ignored and not processed into the internal geometry.
    // Also tests with ST_AsShape and ST_GeometryTypeId are commented out until these functions
    // are implemented. Tests that use ST_Union return serialized geometries in the geometry object
    // with no defined order. For example MULTIPOINT ((2 2), (4 4)) vs MULTIPOINT ((4 4), (2 2)). Even though both are
    // the same, this leads to comparison failures in our tests. Will uncomment once a good solution is in place.
    @Override
    protected void createTables()
    {
        QueryRunner queryRunner = (QueryRunner) getExpectedQueryRunner();
        createPoints(queryRunner);
        createLines(queryRunner);
        createPolygons(queryRunner);
        createMultipoints(queryRunner);
        createMultilines(queryRunner);
        createMultipolygons(queryRunner);
        createCurves(queryRunner);
        createSurfaces(queryRunner);
        createMulticurves(queryRunner);
        createMultisurfaces(queryRunner);
        createCollections(queryRunner);
        createGeometries(queryRunner);
        createPlaces(queryRunner);
        createDepartment(queryRunner);
        createRoads(queryRunner);
        createDrainage(queryRunner);
        createCounties(queryRunner);
        createSaleszones(queryRunner);
        createEveryplace(queryRunner);
        createCollection(queryRunner);
        createGeometryTest(queryRunner);
    }

    @Test
    public void testCheckData()
    {
        String[] tables = {"points", "lines", "polygons", "multipoints", "multilines", "multipolygons", "curves", "surfaces",
                "multicurves", "multisurfaces", "collections", "geometries", "places", "department", "roads", "drainage", "counties",
                "saleszones", "everyplace", "collection"};
        for (String table : tables) {
            assertQuery("SELECT * FROM " + table);
        }
    }

    @Test
    public void testStGeometryFromText()
    {
        // ST_GeomFromText.rxp
        assertQuery("SELECT place_id,cast(ST_AsText(ST_GeometryFromText(place_pt)) as varchar(80)) \"location\" FROM PLACES ORDER BY place_id");
        assertQuery("SELECT road_id,cast(ST_AsText(ST_GeometryFromText(road)) as varchar(256)) \"location\" FROM " +
                "ROADS ORDER BY road_id");
        // ST_Union tests return serialized geometries in the geometry object with no defined order. For example MULTIPOINT ((2 2), (4 4)) vs MULTIPOINT ((4 4), (2 2)). Even though both are the same, this leads to comparison failures in our tests. Will uncomment once a good solution is in place.
//        assertQuery("SELECT sa.place_id,hs.place_id,ST_GeometryFromText(ST_AsText(ST_Union(ST_GeometryFromText(sa.place_pt),ST_GeometryFromText(hs.place_pt)))) \"location\" FROM PLACES sa, PLACES hs ORDER BY sa.place_id, hs.place_id");
//        assertQuery("SELECT sa.road_id,hs.road_id,ST_GeometryFromText(ST_AsText(ST_Union(ST_GeometryFromText(sa.road),ST_GeometryFromText(hs.road)))) \"location\" FROM ROADS sa,ROADS hs WHERE NOT (sa.road_id =10 and hs.road_id = 10) ORDER BY sa.road_id, hs.road_id");
//        assertQuery("SELECT sa.county_id,hs.county_id,ST_GeometryFromText(ST_AsText(ST_Union(ST_GeometryFromText(sa.county),ST_GeometryFromText(hs.county)))) \"location\" FROM COUNTIES sa,COUNTIES hs ORDER BY sa.county_id, hs.county_id");
        assertQuery("SELECT drainage_id,cast(ST_AsText(ST_GeometryFromText(stream_path)) as varchar(256)) \"location\" FROM DRAINAGE ORDER BY drainage_id");
        // Below 4 tests throw a comparison error because 'multipoint' is parsed differently, rather than having an incorrect result...
//        assertQuery("SELECT department_id,ST_AsText(ST_GeometryFromText(employee_loc)) \"location\" FROM department ORDER BY department_id");
//        assertQuery("SELECT saleszone_id,cast(ST_AsText(ST_GeometryFromText(zone_area)) as varchar(512)) \"location\" FROM SALESZONES ORDER BY saleszone_id");
//        assertQuery("SELECT collection_id,ST_AsText(ST_GeometryFromText(collection_multi)) \"location\" FROM COLLECTION ORDER BY collection_id");
//        assertQuery("SELECT everyplace_id,ST_AsText(ST_GeometryFromText(everyplace_geometry)) \"location\" FROM EVERYPLACE ORDER BY everyplace_id");
        // ST_GeomFromText8.rxp
        assertQuery("SELECT id, cast(ST_AsText(ST_GeometryFromText(wkt)) as varchar(120)) \"location\" FROM points WHERE ST_IsEmpty(ST_GeometryFromText(wkt)) = false ORDER BY id");
        assertQuery("SELECT id, cast(ST_AsText(ST_GeometryFromText(wkt)) as varchar(600)) \"location\" FROM polygons WHERE ST_IsEmpty(ST_GeometryFromText(wkt)) = false ORDER BY id");
        assertQuery("SELECT id, cast(ST_AsText(ST_GeometryFromText(wkt)) as varchar(80)) \"location\" FROM points WHERE ST_IsEmpty(ST_GeometryFromText(wkt)) = false ORDER BY id");
        assertQuery("SELECT cast(ST_AsText(ST_GeometryFromText( 'point(10 10)')) as varchar(80)) \"Point\" from points where id=0");
        assertQuery("SELECT cast(ST_AsText(ST_GeometryFromText( 'point m(20 20 15)')) as varchar(80)) \"Point M\" from points where id=0");
        assertQuery("SELECT cast(ST_AsText(ST_GeometryFromText( 'point z(10 20 19)')) as varchar(80)) \"Point Z\" from points where id=0");
        assertQuery("SELECT cast(ST_AsText(ST_GeometryFromText( 'multipoint(10 30, 20 40, 10 5.0E01)')) as varchar(100)) \"MultiPoint\" from points where id=0");
        assertQuery("SELECT cast(ST_AsText(ST_GeometryFromText( 'linestring(10 60, 30 80)')) as varchar(120)) \"LineString\" from points where id=0");
        assertQuery("SELECT cast(ST_AsText(ST_GeometryFromText( 'linestring(10 80, 30 100, 10 100, 30 1.2e+02)')) as varchar(120)) \"LineString\" from points where id=0");
        assertQuery("SELECT cast(ST_AsText(ST_GeometryFromText( 'multilinestring((10 130, 30 130),(10 140, 30 140), (10 150, 30 150))')) as varchar(220)) \"MultiLineString\" from points where id=0");
        assertQuery("SELECT cast(ST_AsText(ST_GeometryFromText( 'polygon((40 20, 60 20, 60 40, 40 40, 40 20))')) as varchar(200)) \"Polygon\" from points where id=0");
        assertQuery("SELECT cast(ST_AsText(ST_GeometryFromText( 'multipolygon(((40 60, 60 60, 60 80, 40 80, 40 60),(40 90, 60 90, 60 110, 40 110, 40 90)))')) as varchar(300)) \"MultiPolygon\" from points where id=0");
        assertQuery("SELECT cast(ST_AsText(ST_GeometryFromText( 'polygon empty' )) as varchar(80)) \"Empty Polygon\" from points where id=0");
        assertQuery("SELECT cast(ST_AsText(ST_GeometryFromText( 'point empty' )) as varchar(80)) \"Empty Point\" from points where id=0");
        assertQuery("SELECT cast(ST_AsText(ST_GeometryFromText( 'multipoint empty' )) as varchar(80)) \"Empty MultiPolygon\" from points where id=0");
        assertQuery("SELECT cast(ST_AsText(ST_GeometryFromText( 'multipolygon(((40 60, 60 60, 60 80, 40 80, 40 60),(40 90, 60 90, 60 110, 40 110, 40 90)))')) as varchar(300)) \"MultiPolygon\" from points where id=0");
        assertQuery("SELECT cast(ST_AsText(ST_GeometryFromText( 'multipolygon(((40 60, 60 60, 60 80, 40 80, 40 60),(40 90, 60 90, 60 110, 40 110, 40 90)))')) as varchar(300)) \"MultiPolygon\" from points where id=0");
        assertQuery("SELECT cast(ST_AsText(ST_GeometryFromText( 'multipoint((10 30), (20 40), (10 5.0E01))')) as varchar(100)) \"MultiPoint\"");
    }

    @Test
    public void testStX()
    {
        // ST_X01.rxp
        assertQuery("SELECT place_ID,round(ST_X(ST_GeometryFromText(place_pt)),10) \"X_coordinate\" FROM PLACES ORDER BY place_ID");
        assertQuery("SELECT place_ID,place_pt \"Point\" FROM PLACES WHERE ST_X(ST_GeometryFromText(place_pt)) > 10 ORDER BY place_ID");
        // ST_X02.rxp
        assertQuery("SELECT point_ID,round(ST_X(ST_GeometryFromText(point)),10) \"X_Coordinate\" FROM STPOINT_TEST ORDER BY point_ID");
        // ST_X03.rxp
        assertQueryFails("SELECT road_ID,ST_X(ST_GeometryFromText(road)) \"X_Coordinate\" FROM ROADS Order by road_ID", " ST_X requires a Point geometry, found LineString Top-level Expression: presto.default.st_x(presto.default.st_geometryfromtext(road))");
        assertQueryFails("SELECT point_ID,ST_X(13.23) \"X_Coordinate\" FROM STPOINT_TEST Order by point_ID", "line 1:17: Unexpected parameters (decimal(4,2)) for function st_x. Expected: st_x(Geometry) ");
        assertQuery("SELECT point_ID,ST_X(NULL) \"X_Coordinate\" FROM STPOINT_TEST Order by point_ID");
        // ST_X08.rxp
        assertQuery("SELECT id,ST_X(ST_GeometryFromText(wkt)) \"X-coordinate\" FROM points ORDER BY ID");
        assertQuery("SELECT ID, cast(wkt as varchar(100)) \"Old Point\", cast(ST_X(ST_GeometryFromText(wkt)) as varchar(100)) \"New Point\" FROM points where ST_IsEmpty(ST_GeometryFromText(wkt))=false ORDER BY ID");
        assertQuery("SELECT ID,wkt \"Points with X>=10\" FROM points WHERE ST_X(ST_GeometryFromText(wkt)) >= 10 ORDER BY ID");
    }

    @Test
    public void testStY()
    {
        // ST_Y01.rxp
        assertQuery("SELECT place_ID,round(ST_Y(ST_GeometryFromText(place_pt)),10) \"Y_coordinate\" FROM PLACES ORDER BY place_ID");
        assertQuery("SELECT place_ID,place_pt \"Point\" FROM PLACES WHERE ST_Y(ST_GeometryFromText(place_pt)) > 10 ORDER BY place_ID");
        // ST_Y02.rxp
        assertQuery("SELECT point_ID,round(ST_Y(ST_GeometryFromText(point)),10) \"Y_Coordinate\" FROM STPOINT_TEST ORDER BY point_ID");
        // ST_Y03.rxp
        assertQueryFails("SELECT road_ID,ST_Y(ST_GeometryFromText(road)) \"Y_Coordinate\" FROM ROADS Order by road_ID", " ST_Y requires a Point geometry, found LineString Top-level Expression: presto.default.st_y(presto.default.st_geometryfromtext(road))");
        assertQueryFails("SELECT point_ID,ST_Y(13.23) \"Y_Coordinate\" FROM STPOINT_TEST Order by point_ID", "line 1:17: Unexpected parameters (decimal(4,2)) for function st_y. Expected: st_y(Geometry) ");
        assertQuery("SELECT point_ID,ST_Y(NULL) \"Y_Coordinate\" FROM STPOINT_TEST Order by point_ID");
        // ST_Y08.rxp
        assertQuery("SELECT id,ST_Y(ST_GeometryFromText(wkt)) \"Y-coordinate\" FROM points ORDER BY ID");
        assertQuery("SELECT ID, cast(wkt as varchar(100)) \"Old Point\", cast(ST_Y(ST_GeometryFromText(wkt)) as varchar(100)) \"New Point\" FROM points where ST_IsEmpty(ST_GeometryFromText(wkt))=false ORDER BY ID");
        assertQuery("SELECT ID,wkt \"Points with Y>=10\" FROM points WHERE ST_Y(ST_GeometryFromText(wkt)) >= 10 ORDER BY ID");
    }

    @Test
    public void testStPoint()
    {
        // ST_Point8.rxp
        // Commenting out below tests because it uses ST_AsShape. Refer to NOTE at the top for more details.
//        assertQuery("SELECT id, cast(ST_AsText(ST_Point(ST_AsShape(wkt),0 )) as varchar(120)) \"location\" FROM points WHERE ST_IsEmpty(ST_GeometryFromText(wkt)) = false ORDER BY id");
//        assertQuery("SELECT id, cast(ST_AsText(ST_Point(ST_AsShape(wkt), 1 )) as varchar(80)) \"location\" FROM points WHERE ST_IsEmpty(ST_GeometryFromText(wkt)) = false and id < 7 ORDER BY id");
        // Unexpected parameters (Geometry) for function st_point. Expected: st_point(double, double)
//        assertQuery("SELECT ST_AsText(ST_Point(ST_GeomFromBinary(x'0100000000000000000024400000000000002440'))) \"Point\" from points where id=0");

        // ST_Point8_wkb.rxp
        // Unexpected parameters (varbinary, integer) for function st_point.
//        assertQuery("SELECT id, cast(ST_AsText(ST_Point(ST_AsBinary(ST_GeometryFromText(wkt)),0 )) as varchar(80)) \"location\" FROM points WHERE ST_IsEmpty(ST_GeometryFromText(wkt)) = false ORDER BY id");
//        assertQuery("SELECT id, cast(ST_AsText(ST_Point(ST_AsBinary(ST_GeometryFromText(wkt)), 12 )) as varchar(80)) \"location\" FROM points WHERE ST_IsEmpty(ST_GeometryFromText(wkt)) = false ORDER BY id");
        // Wrong parameter of blob as well ST_Point only takes in double and double.
//        assertQuery("SELECT id, cast(ST_AsText(ST_Point(blob)) as varchar(100)) \"Point\" FROM TABLE ( VALUES ( 1, BLOB(x'010100000000000000000024400000000000002440') ), ( 2, BLOB(x'000000000140240000000000004024000000000000') ) ) AS t(id, blob) ORDER BY id");
//        assertQuery("SELECT id, cast(ST_AsText(ST_Point(blob, 0)) as varchar(100)) \"Point\" FROM   TABLE ( VALUES ( 1, BLOB(x'010100000000000000000024400000000000002440') ), ( 2, BLOB(x'000000000140240000000000004024000000000000') ) ) AS t(id, blob) ORDER BY id");

        // ST_Point8_wkt.rxp
        // Unexpected parameters (varchar, integer) for function ST_Point.
//        assertQuery("SELECT id, cast(ST_AsText(ST_Point(wkt,0 )) as varchar(180)) \"location\" FROM points WHERE ST_IsEmpty(ST_GeometryFromText(wkt)) = false ORDER BY id");
//        assertQuery("SELECT id, cast(ST_AsText(ST_Point(wkt)) as varchar(180)) \"location\" FROM points WHERE ST_IsEmpty(ST_GeometryFromText(wkt)) = false ORDER BY id");
//        assertQuery("SELECT cast(ST_AsText(ST_Point('point(10 10)')) as varchar(80)) \"Point\" from points where id=0");
//        assertQuery("SELECT cast(ST_AsText(ST_Point('point m(20 20 15)')) as varchar(80)) \"Point M\" from points where id=0");
        assertQuery("SELECT CAST(ST_AsText(ST_Point(15.01, 22.03)) AS VARCHAR(40)) AS \"Point\" FROM places WHERE place_ID = 1");
        assertQuery("SELECT CAST(ST_AsText(ST_Point(-15.01, -22.03)) as varchar(40)) \"Point\" FROM PLACES WHERE place_ID = 1");
        assertEquals(computeScalar("SELECT ST_Point(15.01, 22.03)"), "POINT (15.01 22.03)");
        assertQuery("SELECT point_id, ST_AsText(ST_Point(15.01, 22.03)) AS \"Point\" FROM STPOINT_TEST WHERE point_id = 111 ORDER BY point_id");
        assertQuery("SELECT point_id, ST_AsText(ST_Point(21473836,22)) AS \"Point\" FROM STPOINT_TEST WHERE point_id = 111 ORDER BY point_id");
        assertQuery("SELECT point_id, ST_AsText(ST_Point(21473,21472836)) AS \"Point\" FROM STPOINT_TEST WHERE point_id = 111 ORDER BY point_id");
        assertQuery("SELECT point_id, ST_AsText(ST_Point(21473836,21472836)) AS \"Point\" FROM STPOINT_TEST WHERE point_id = 111 ORDER BY point_id");
        assertQuery("SELECT point_id, ST_AsText(ST_Point(0.111111111111111111111111111111,22.2222222222222222222222222222)) AS \"Point\" FROM STPOINT_TEST WHERE point_id = 111 ORDER BY point_id");
        assertQuery("SELECT point_id, ST_AsText(ST_Point(0,0)) AS \"Point\" FROM STPOINT_TEST WHERE point_id = 111 ORDER BY point_id");
        assertQuery("SELECT point_id, ST_AsText(ST_Point(-11.11,-22.22)) AS \"Point\" FROM STPOINT_TEST WHERE point_id = 111 ORDER BY point_id");
        assertQuery("SELECT point_id, ST_AsText(ST_Point(-1000, -2000)) AS \"Point\" FROM STPOINT_TEST WHERE point_id = 111 ORDER BY point_id");
    }

    @Test
    public void testStArea()
    {
        // st_area.rxp
        assertQuery("SELECT name, ST_Area(ST_GeometryFromText(county)) area FROM counties ORDER BY name");
        assertQuery("SELECT name, ST_Area(ST_GeometryFromText(county)) area FROM counties WHERE ST_Area(ST_GeometryFromText(county)) > 0 ORDER BY name");
        // st_area2.rxp
        assertQuery("SELECT c1.county_id, c2.county_id, ST_Area(ST_Union(ST_GeometryFromText(c1.county), ST_GeometryFromText(c2.county))) AS area " +
                "FROM counties AS c1, counties AS c2 " +
                "order by c1.county_id, c2.county_id");
        // Commenting out below test because it uses ST_Union. Refer to NOTE at the top for more details.
//        assertQuery("SELECT c1.county_id, c2.county_id, ST_Area(ST_Union(ST_GeometryFromText(c1.county), ST_GeometryFromText(c2.county))) AS area " +
//                "FROM counties AS c1, counties AS c2 WHERE ST_GeometryType(ST_Union(ST_GeometryFromText(c1.county), ST_GeometryFromText(c2.county))) IN ('ST_MultiPolygon', 'MultiPolygon') " +
//                "order by c1.county_id, c2.county_id");
        // st_area2_asr.rxp
        assertQuery("SELECT saleszone_id, ST_Area(ST_GeometryFromText(zone_area)) FROM saleszones ORDER BY saleszone_id");
        //st_area2_blu.rxp -> ST_GeometryTypeId not registered yet...can change to geometryType if i know what the numbers id represent like line multipolygon...??
//        assertQuery("SELECT c1.county_id, c2.county_id, ST_Area(ST_Union(ST_GeometryFromText(c1.county), ST_GeometryFromText(c2.county))) AS area FROM counties as c1, counties as c2 " +
//                "WHERE ST_GeometryTypeId(ST_Union(ST_GeometryFromText(c1.county), ST_GeometryFromText(c2.county))) IN ('MultiPolygon', '24','25','26','27') " +
//                "ORDER BY c1.county_id, c2.county_id");
        // st_area8.rxp
        assertQuery("SELECT id, ST_Area(ST_GeometryFromText(wkt)) area FROM surfaces ORDER BY id");
        // st_area8a.rxp is repeat of above test.
        // st_area_asr.rxp
        assertQuery("SELECT name, ST_Area(ST_GeometryFromText(county)) area FROM counties ORDER BY name");
        assertQuery("SELECT name, ST_Area(ST_GeometryFromText(county)) area FROM counties WHERE ST_Area(ST_GeometryFromText(county)) > 0 ORDER BY NAME");
        // st_area_m8.rxp
        assertQuery("select id, ST_area(ST_GeometryFromText(wkt)) area from multisurfaces order by id");
    }

    @Test
    public void testStWithin()
    {
        // st_within.rxp
        assertQuery("SELECT r1.road_id \"line1\", r2.road_id \"line2\", ST_within(ST_GeometryFromText(r1.road), ST_GeometryFromText(r2.road)) \"within(line1,line2)\", " +
                "ST_within(ST_GeometryFromText(r2.road), ST_GeometryFromText(r1.road)) \"within(line2,line1)\" from roads r1, roads r2 order by r1.road_id, r2.road_id");
        assertQuery("select c1.county_id \"polygon1\", c2.county_id \"polygon2\", ST_within(ST_GeometryFromText(c1.county), ST_GeometryFromText(c2.county)) \"within(poly1,poly2)\", ST_within(ST_GeometryFromText(c2.county), ST_GeometryFromText(c1.county)) \"within(poly2,poly1)\" from counties c1, counties c2 order by c1.county_id, c2.county_id");
        assertQuery("select p1.place_id \"point1\", p2.place_id \"point2\", ST_within(ST_GeometryFromText(p1.place_pt), ST_GeometryFromText(p2.place_pt)) \"within(point1,point2)\", ST_within(ST_GeometryFromText(p2.place_pt), ST_GeometryFromText(p1.place_pt)) \"within(point2,point1)\" from places p1, places p2 order by p1.place_id, p2.place_id");
        assertQuery("select place_id, county_id, ST_within(ST_GeometryFromText(place_pt), ST_GeometryFromText(county)) \"within(point,poly)\" from places, counties order by place_id, county_id");
        assertQuery("select road_id, county_id, ST_within(ST_GeometryFromText(road), ST_GeometryFromText(county)) \"within(line,poly)\" from roads, counties order by road_id, county_id");
        assertQuery("select place_id, road_id, ST_within(ST_GeometryFromText(place_pt), ST_GeometryFromText(road)) \"within(point,line)\" from roads, places order by place_id, road_id");
        assertQuery("select road_id, county_id, ST_within(ST_GeometryFromText(road), ST_GeometryFromText(county)) \"within(line,poly)\" from roads, counties where ST_within(ST_GeometryFromText(road),ST_GeometryFromText(county)) = true order by road_id, county_id");
        assertQuery("select road_id, county_id, ST_within(ST_GeometryFromText(road), ST_GeometryFromText(county)) \"within(line,poly)\" from roads, counties where ST_within(ST_GeometryFromText(road),ST_GeometryFromText(county)) = false order by road_id, county_id");
        assertQuery("select place_id, road_id, ST_within(ST_GeometryFromText(place_pt), ST_GeometryFromText(road)) \"within(point,line)\" from roads, places where ST_within(ST_GeometryFromText(place_pt), ST_GeometryFromText(road)) = true order by place_id, road_id");
        assertQuery("select road_id,saleszone_id, ST_within(ST_GeometryFromText(road),ST_GeometryFromText(zone_area)) \"within(line,multipoly)\" from saleszones, roads order by road_id, saleszone_id");
        assertQuery("select drainage_id,county_id, ST_within(ST_GeometryFromText(stream_path),ST_GeometryFromText(county)) \"within(multiline,poly)\" from drainage, counties order by drainage_id, county_id");
        assertQuery("select department_id, county_id, ST_within(ST_GeometryFromText(employee_loc), ST_GeometryFromText(county)) \"within(multipoint,poly)\" from department, counties order by department_id, county_id");
        // st_within8.rxp
        assertQuery("select l1.id \"line1\", l2.id \"line2\", ST_within(ST_GeometryFromText(l1.wkt), ST_GeometryFromText(l2.wkt)) \"within(line1,line2)\", ST_within(ST_GeometryFromText(l2.wkt), ST_GeometryFromText(l1.wkt)) \"within(line2,line1)\" from lines l1, lines l2 where l1.id >= l2.id and l1.id < 600 order by l1.id, l2.id");
        assertQuery("select p1.id \"poly1\", p2.id \"poly2\", ST_within(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)) \"within(poly1,poly2)\", ST_within(ST_GeometryFromText(p2.wkt), ST_GeometryFromText(p1.wkt)) \"within(poly2,poly1)\" from polygons p1, polygons p2 where p1.id >= p2.id and p1.id < 1200 order by p1.id, p2.id");
        assertQuery("select p1.id \"poly\", p2.id \"point\", ST_within(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)) \"within(poly,point)\", ST_within(ST_GeometryFromText(p2.wkt), ST_GeometryFromText(p1.wkt)) \"within(point,poly)\" from polygons p1, points p2 where p1.id < 1004 and p2.id < 4 order by p1.id, p2.id");
    }

    @Test
    public void testStIntersects()
    {
        // st_intersects.rxp
        assertQuery("select r1.road_id \"line1\", r2.road_id \"line2\", ST_intersects(ST_GeometryFromText(r1.road), ST_GeometryFromText(r2.road)) \"intersects(line1,line2)\" , ST_intersects(ST_GeometryFromText(r2.road), ST_GeometryFromText(r1.road)) \"intersects(line2,line1)\" from roads r1, roads r2 order by r1.road_id, r2.road_id");
        assertQuery("select c1.county_id \"polygon1\", c2.county_id \"polygon2\", ST_intersects(ST_GeometryFromText(c1.county), ST_GeometryFromText(c2.county)) \"intersects(poly1,poly2)\", ST_intersects(ST_GeometryFromText(c2.county), ST_GeometryFromText(c1.county)) \"intersects(poly2,poly1)\" from counties c1, counties c2 order by c1.county_id,  c2.county_id");
        assertQuery("select  place_id, county_id, ST_intersects(ST_GeometryFromText(place_pt), ST_GeometryFromText(county)) \"intersects(point,poly)\", ST_intersects(ST_GeometryFromText(county),ST_GeometryFromText(place_pt)) \"intersects(poly,point)\" from places , counties order by place_id, county_id");
        assertQuery("select place_id,road_id, ST_intersects(ST_GeometryFromText(place_pt), ST_GeometryFromText(road)) \"intersects(point,line)\", ST_intersects(ST_GeometryFromText(road),ST_GeometryFromText(place_pt)) \"intersects(line,point)\" from roads, places order by place_id, road_id");
        assertQuery("select county_id,road_id, ST_intersects(ST_GeometryFromText(county), ST_GeometryFromText(road)) \"intersects(poly,line)\", ST_intersects(ST_GeometryFromText(road),ST_GeometryFromText(county)) \"intersects(line,poly)\" from roads, counties order by county_id, road_id");
        assertQuery("select county_id, road_id, ST_intersects(ST_GeometryFromText(county), ST_GeometryFromText(road)) \"intersects(poly,line)\" from roads, counties where ST_intersects(ST_GeometryFromText(county), ST_GeometryFromText(road)) = false order by county_id, road_id");
        assertQuery("select r1.road_id, r2.road_id, ST_intersects(ST_GeometryFromText(r1.road), ST_GeometryFromText(r2.road)) \"intersects(line,line)\" from roads r1, roads r2 where ST_intersects(ST_GeometryFromText(r1.road), ST_GeometryFromText(r2.road)) = true order by r1.road_id, r2.road_id");
        assertQuery("select c1.county_id, c2.county_id, ST_intersects(ST_GeometryFromText(c1.county), ST_GeometryFromText(c2.county)) \"intersects(poly,poly)\" from counties c1, counties c2 where ST_intersects(ST_GeometryFromText(c1.county), ST_GeometryFromText(c2.county)) = true order by c1.county_id,  c2.county_id");
        assertQuery("select saleszone_id, road_id, ST_intersects(ST_GeometryFromText(zone_area), ST_GeometryFromText(road)) \"intersects(multipoly,line)\" from saleszones, roads order by saleszone_id, road_id");
        assertQuery("select drainage_id,county_id, ST_intersects(ST_GeometryFromText(stream_path), ST_GeometryFromText(county)) \"intersects(multiline,poly)\" from drainage , counties order by drainage_id, county_id");
        assertQuery("select department_id, county_id, ST_intersects(ST_GeometryFromText(employee_loc), ST_GeometryFromText(county)) \"intersects(multipoint,poly)\" from department, counties order by department_id, county_id");
        // st_intersects8.rxp
        assertQuery("select l1.id \"line1\", l2.id \"line2\", ST_intersects(ST_GeometryFromText(l1.wkt), ST_GeometryFromText(l2.wkt)) \"intersects(line1,line2)\", ST_intersects(ST_GeometryFromText(l2.wkt), ST_GeometryFromText(l1.wkt)) \"intersects(line2,line1)\" from lines l1, lines l2 where l1.id >= l2.id and l1.id<600 order by l1.id, l2.id");
        assertQuery("select p1.id \"poly1\", p2.id \"poly2\", ST_intersects(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)) \"intersects(poly1,poly2)\", ST_intersects(ST_GeometryFromText(p2.wkt), ST_GeometryFromText(p1.wkt)) \"intersects(poly2,poly1)\" from polygons p1, polygons p2 where p1.id >= p2.id and p1.id < 1200 order by p1.id, p2.id");
        assertQuery("select p1.id \"poly\", p2.id \"point\", ST_intersects(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)) \"intersects(poly,point)\", ST_intersects(ST_GeometryFromText(p2.wkt), ST_GeometryFromText(p1.wkt)) \"intersects(point,poly)\" from polygons p1, points p2 where p1.id < 1004 and p2.id < 4 order by p1.id, p2.id");
        assertQuery("select p1.id \"poly1\", p2.id \"poly2\" from polygons p1, polygons p2 where ST_intersects(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)) = true order by p1.id, p2.id");
        // st_intersects_asr.rxp
        assertQuery("select r1.road_id, r2.road_id, ST_intersects(ST_GeometryFromText(r1.road), ST_GeometryFromText(r2.road)) \"intersects(line1,line2)\", ST_intersects(ST_GeometryFromText(r2.road), ST_GeometryFromText(r1.road)) \"intersects(line2,line1)\" from roads r1, roads r2 order by r1.road_id, r2.road_id");
        assertQuery("select count(*) from (select c1.county_id, c2.county_id from counties c1, counties c2 where ST_XMax(ST_GeometryFromText(c1.county)) < 62.0 order by c1.county_id,  c2.county_id)");
        assertQuery("select place_id, county_id, ST_intersects(ST_GeometryFromText(place_pt), ST_GeometryFromText(county)) \"intersects(point,poly)\", ST_intersects(ST_GeometryFromText(county),ST_GeometryFromText(place_pt)) \"intersects(poly,point)\" from places, counties order by place_id, county_id");
        assertQuery("select place_id,road_id, ST_intersects(ST_GeometryFromText(place_pt), ST_GeometryFromText(road)) \"intersects(point,line)\", ST_intersects(ST_GeometryFromText(road),ST_GeometryFromText(place_pt)) \"intersects(line,point)\" from roads, places order by place_id, road_id");
        assertQuery("select county_id,road_id, ST_intersects(ST_GeometryFromText(county), ST_GeometryFromText(road)) \"intersects(poly,line)\", ST_intersects(ST_GeometryFromText(road),ST_GeometryFromText(county)) \"intersects(line,poly)\" from roads, counties order by county_id, road_id");
        assertQuery("select county_id, road_id, ST_intersects(ST_GeometryFromText(county), ST_GeometryFromText(road)) \"intersects(poly,line)\" from roads, counties where ST_intersects(ST_GeometryFromText(county),ST_GeometryFromText(road)) = false order by county_id, road_id");
        assertQuery("select r1.road_id, r2.road_id, ST_intersects(ST_GeometryFromText(r1.road), ST_GeometryFromText(r2.road)) \"intersects(line,line)\" from roads r1, roads r2 where ST_intersects(ST_GeometryFromText(r1.road) , ST_GeometryFromText(r2.road)) = true order by r1.road_id, r2.road_id");
        assertQuery("select c1.county_id, c2.county_id, ST_intersects(ST_GeometryFromText(c1.county), ST_GeometryFromText(c2.county)) \"intersects(poly,poly)\" from counties c1, counties c2 where ST_intersects(ST_GeometryFromText(c1.county), ST_GeometryFromText(c2.county)) = true order by c1.county_id, c2.county_id");
        assertQuery("select saleszone_id, road_id, ST_intersects(ST_GeometryFromText(zone_area), ST_GeometryFromText(road)) \"intersects(multipoly,line)\" from saleszones, roads order by saleszone_id, road_id");
        assertQuery("select drainage_id,county_id, ST_intersects(ST_GeometryFromText(stream_path), ST_GeometryFromText(county)) \"intersects(multiline,poly)\" from drainage, counties order by drainage_id, county_id");
        assertQuery("select department_id, county_id, ST_intersects(ST_GeometryFromText(employee_loc), ST_GeometryFromText(county)) \"intersects(multipoint,poly)\" from department, counties order by department_id, county_id");
    }

    @Test
    public void testStMinMax()
    {
        // ST_MinMax08.rxp
        assertQuery("SELECT id, ST_XMin(ST_GeometryFromText(wkt)) \"MinX\", ST_YMin(ST_GeometryFromText(wkt)) \"MinY\", ST_XMax(ST_GeometryFromText(wkt)) \"MaxX\", ST_YMax(ST_GeometryFromText(wkt)) \"MaxY\" FROM geometries ORDER BY ID");
    }

    @Test
    public void testStLineFromText()
    {
        // ST_LineFromText.rxp
        assertQuery("SELECT road_id, cast(ST_AsText(ST_LineFromText(road)) as varchar(256)) \"location\" FROM ROADS ORDER BY road_id");
        // ST_LineFromText8.rxp
        assertQuery("SELECT id, cast(ST_AsText(ST_LineFromText(wkt)) as varchar(220)) \"location\" FROM lines WHERE ST_IsEmpty(ST_GeometryFromText(wkt)) = false ORDER BY id");
        assertQuery("SELECT cast(ST_AsText(ST_LineFromText('linestring(10 60, 30 80)')) as varchar(120)) \"LineString\" from points where id=0");
        assertQuery("SELECT cast(ST_AsText(ST_LineFromText('linestring(10 80, 30 100, 10 100, 30 1.2e+02)')) as varchar(120)) \"LineString\" from points where id=0");
    }

    @Test
    public void testStContains()
    {
        // st_contains.rxp
        assertQuery("select r1.road_id \"line1\", r2.road_id \"line2\", ST_contains(ST_GeometryFromText(r1.road), ST_GeometryFromText(r2.road)) \"contains(line1,line2)\", ST_contains(ST_GeometryFromText(r2.road), ST_GeometryFromText(r1.road)) \"contains(line2,line1)\" from roads r1, roads r2 order by r1.road_id , r2.road_id");
        assertQuery("select c1.county_id \"polygon1\", c2.county_id \"polygon2\", ST_contains(ST_GeometryFromText(c1.county), ST_GeometryFromText(c2.county)) \"contains(poly1,poly2)\", ST_contains(ST_GeometryFromText(c2.county), ST_GeometryFromText(c1.county)) \"contains(poly2,poly1)\" from counties c1, counties c2 order by c1.county_id, c2.county_id");
        assertQuery("select p1.place_id \"point1\", p2.place_id \"point2\", ST_contains(ST_GeometryFromText(p1.place_pt), ST_GeometryFromText(p2.place_pt)) \"contains(point1,point2)\", ST_contains(ST_GeometryFromText(p2.place_pt), ST_GeometryFromText(p1.place_pt)) \"contains(point2,point1)\" from places p1, places p2 order by p1.place_id, p2.place_id");
        assertQuery("select place_id, county_id, ST_contains(ST_GeometryFromText(county),ST_GeometryFromText(place_pt)) \"contains(poly,point)\" from places, counties order by place_id, county_id");
        assertQuery("select road_id, county_id, ST_contains(ST_GeometryFromText(county), ST_GeometryFromText(road)) \"contains(poly,line)\" from roads, counties order by road_id, county_id");
        assertQuery("select place_id, road_id, ST_contains(ST_GeometryFromText(road),ST_GeometryFromText(place_pt)) \"contains(line,point)\" from roads, places order by place_id, road_id");
        assertQuery("select road_id, county_id, ST_contains(ST_GeometryFromText(county), ST_GeometryFromText(road)) \"contains(poly,line)\" from roads, counties where ST_contains(ST_GeometryFromText(county),ST_GeometryFromText(road)) = true order by road_id, county_id");
        assertQuery("select road_id, county_id, ST_contains(ST_GeometryFromText(county),ST_GeometryFromText(road)) \"contains(poly,line)\" from roads, counties where ST_contains(ST_GeometryFromText(county),ST_GeometryFromText(road)) = false order by road_id, county_id");
        assertQuery("select place_id, road_id, ST_contains(ST_GeometryFromText(road),ST_GeometryFromText(place_pt)) \"contains(line,point)\" from roads, places where ST_contains(ST_GeometryFromText(road),ST_GeometryFromText(place_pt)) = true order by place_id, road_id");
        assertQuery("select road_id,saleszone_id, ST_contains(ST_GeometryFromText(zone_area),ST_GeometryFromText(road)) \"contains(multipoly,line)\" from saleszones, roads order by road_id,saleszone_id");
        assertQuery("select drainage_id,county_id, ST_contains(ST_GeometryFromText(county),ST_GeometryFromText(stream_path)) \"contains(poly,multiline)\" from drainage, counties order by drainage_id,county_id");
        assertQuery("select department_id, county_id, ST_contains(ST_GeometryFromText(county),ST_GeometryFromText(employee_loc)) \"contains(poly,multipoint)\" from department, counties order by department_id, county_id");
        // st_contains8.rxp
        assertQuery("select l1.id \"line1\", l2.id \"line2\", ST_contains(ST_GeometryFromText(l1.wkt), ST_GeometryFromText(l2.wkt)) \"contains(line1,line2)\", ST_contains(ST_GeometryFromText(l2.wkt), ST_GeometryFromText(l1.wkt)) \"contains(line2,line1)\" from lines l1, lines l2 where l1.id >= l2.id and l1.id < 600 order by l1.id, l2.id");
        assertQuery("select p1.id \"poly1\", p2.id \"poly2\", ST_contains(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)) \"contains(poly1,poly2)\", ST_contains(ST_GeometryFromText(p2.wkt), ST_GeometryFromText(p1.wkt)) \"contains(poly2,poly1)\" from polygons p1, polygons p2 where p1.id >= p2.id and p1.id < 1200 order by p1.id, p2.id");
        assertQuery("select p1.id \"poly\", p2.id \"point\", ST_contains(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)) \"contains(poly,point)\", ST_contains(ST_GeometryFromText(p2.wkt), ST_GeometryFromText(p1.wkt)) \"contains(point,poly)\" from polygons p1, points p2 where p1.id < 1004 and p2.id < 4 order by p1.id, p2.id");
        assertQuery("select ST_contains(ST_GeometryFromText('linestring (1 0, 1 4)') ,ST_GeometryFromText('multipolygon (((1 1, 3 1, 3 3, 1 1)),((4 1, 6 1, 6 3, 4 3, 4 1)))'))");
    }

    @Test
    public void testStCrosses()
    {
        // st_crosses.rxp
        assertQuery("select r1.road_id, r2.road_id, ST_crosses(ST_GeometryFromText(r1.road), ST_GeometryFromText(r2.road)) \"crosses(line1,line2)\", ST_crosses(ST_GeometryFromText(r2.road), ST_GeometryFromText(r1.road)) \"crosses(line2,line1)\" from roads r1, roads r2 order by r1.road_id, r2.road_id");
        assertQuery("select c1.county_id, c2.county_id, ST_crosses(ST_GeometryFromText(c1.county), ST_GeometryFromText(c2.county)) \"crosses(poly1,poly2)\", ST_crosses(ST_GeometryFromText(c2.county), ST_GeometryFromText(c1.county)) \"crosses(poly2,poly1)\" from counties c1, counties c2 order by c1.county_id, c2.county_id");
        assertQuery("select place_id, county_id, ST_crosses(ST_GeometryFromText(place_pt), ST_GeometryFromText(county)) \"crosses(point,poly)\", ST_crosses(ST_GeometryFromText(county),ST_GeometryFromText(place_pt)) \"crosses(poly,point)\" from places, counties order by place_id, county_id");
        assertQuery("select place_id,road_id, ST_crosses(ST_GeometryFromText(place_pt), ST_GeometryFromText(road)) \"crosses(point,line)\", ST_crosses(ST_GeometryFromText(road),ST_GeometryFromText(place_pt)) \"crosses(line,point)\" from roads, places order by place_id,road_id");
        assertQuery("select county_id,road_id, ST_crosses(ST_GeometryFromText(county), ST_GeometryFromText(road)) \"crosses(poly,line)\", ST_crosses(ST_GeometryFromText(road),ST_GeometryFromText(county)) \"crosses(line,poly)\" from roads, counties order by county_id, road_id");
        assertQuery("select county_id, road_id, ST_crosses(ST_GeometryFromText(county), ST_GeometryFromText(road)) \"crosses(poly,line)\" from roads, counties where ST_crosses(ST_GeometryFromText(county), ST_GeometryFromText(road)) = false order by county_id, road_id");
        assertQuery("select r1.road_id, r2.road_id, ST_crosses(ST_GeometryFromText(r1.road), ST_GeometryFromText(r2.road)) \"crosses(line,line)\" from roads r1, roads r2 where ST_crosses(ST_GeometryFromText(r1.road), ST_GeometryFromText(r2.road)) = true order by r1.road_id,r2.road_id");
        assertQuery("select c1.county_id, c2.county_id, ST_crosses(ST_GeometryFromText(c1.county), ST_GeometryFromText(c2.county)) \"crosses(poly,poly)\" from counties c1, counties c2 where ST_crosses(ST_GeometryFromText(c1.county), ST_GeometryFromText(c2.county)) = true order by c1.county_id,c2.county_id");
        assertQuery("select saleszone_id, road_id, ST_crosses(ST_GeometryFromText(zone_area), ST_GeometryFromText(road)) \"crosses(multipoly,line)\" from saleszones, roads order by saleszone_id, road_id");
        assertQuery("select drainage_id,county_id, ST_crosses(ST_GeometryFromText(stream_path), ST_GeometryFromText(county)) \"crosses(multiline,poly)\" from drainage, counties order by drainage_id, county_id");
        assertQuery("select department_id, county_id, ST_crosses(ST_GeometryFromText(employee_loc), ST_GeometryFromText(county)) \"crosses(multipoint,poly)\" from department, counties order by department_id, county_id");
        // st_crosses8.rxp
        assertQuery("select l1.id \"line1\", l2.id \"line2\", ST_crosses(ST_GeometryFromText(l1.wkt), ST_GeometryFromText(l2.wkt)) \"crosses(line1,line2)\", ST_crosses(ST_GeometryFromText(l2.wkt), ST_GeometryFromText(l1.wkt)) \"crosses(line2,line1)\" from lines l1, lines l2 where l1.id >= l2.id and l1.id < 600 order by l1.id, l2.id");
        assertQuery("select p1.id \"poly1\", l2.id \"line2\", ST_crosses(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(l2.wkt)) \"crosses(poly1,line2)\", ST_crosses(ST_GeometryFromText(l2.wkt), ST_GeometryFromText(p1.wkt)) \"crosses(line2,poly1)\" from polygons p1, lines l2 where p1.id < 1200 and l2.id < 600 order by p1.id, l2.id");
        assertQuery("select p1.id \"poly\", p2.id \"mpoint\", ST_crosses(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)) \"crosses(poly,mpoint)\", ST_crosses(ST_GeometryFromText(p2.wkt), ST_GeometryFromText(p1.wkt)) \"crosses(mpoint,poly)\" from polygons p1, multipoints p2 where p1.id < 1004 order by p1.id, p2.id");
        assertQuery("select ST_crosses(ST_GeometryFromText('linestring (1 0, 1 4)'),ST_GeometryFromText('multipolygon (((1 1, 3 1, 3 3, 1 1)),((4 1, 6 1, 6 3, 4 3, 4 1)))'))");
    }

    @Test
    public void testStDisjoint()
    {
        // st_disjoint.rxp
        assertQuery("select l1.road_id,l2.road_id, ST_disjoint(ST_GeometryFromText(l1.road), ST_GeometryFromText(l2.road)) \"disjoint(line,line)\" from roads l1, roads l2 order by l1.road_id,l2.road_id");
        assertQuery("select c1.county_id,c2.county_id, ST_disjoint(ST_GeometryFromText(c1.county), ST_GeometryFromText(c2.county)) \"disjoint(poly,poly)\" from counties c1, counties c2 order by c1.county_id,c2.county_id");
        assertQuery("select p1.place_id,p2.place_id, ST_disjoint(ST_GeometryFromText(p1.place_pt), ST_GeometryFromText(p2.place_pt)) \"disjoint(point,point)\" from places p1, places p2 order by p1.place_id,p2.place_id");
        assertQuery("select place_id, county_id, ST_disjoint(ST_GeometryFromText(place_pt), ST_GeometryFromText(county)) \"disjoint(point,poly)\", ST_disjoint(ST_GeometryFromText(county),ST_GeometryFromText(place_pt)) \"disjoint(poly,point)\" from places, counties order by place_id, county_id");
        assertQuery("select place_id, road_id, ST_disjoint(ST_GeometryFromText(place_pt), ST_GeometryFromText(road)) \"disjoint(point,line)\", ST_disjoint(ST_GeometryFromText(road),ST_GeometryFromText(place_pt)) \"disjoint(line,point)\" from roads, places order by place_id, road_id");
        assertQuery("select county_id, road_id, ST_disjoint(ST_GeometryFromText(county), ST_GeometryFromText(road)) \"disjoint(poly,line)\", ST_disjoint(ST_GeometryFromText(road),ST_GeometryFromText(county)) \"disjoint(line,poly)\" from roads, counties order by county_id, road_id");
        assertQuery("select county_id, road_id, ST_disjoint(ST_GeometryFromText(county), ST_GeometryFromText(road)) disjoint from roads, counties where ST_disjoint(ST_GeometryFromText(county), ST_GeometryFromText(road)) = true order by county_id, road_id");
        assertQuery("select place_id, road_id, ST_disjoint(ST_GeometryFromText(place_pt), ST_GeometryFromText(road)) disjoint from roads, places where ST_disjoint(ST_GeometryFromText(place_pt), ST_GeometryFromText(road)) = true and ST_disjoint(ST_GeometryFromText(place_pt), ST_GeometryFromText(road)) = false order by place_id, road_id");
        assertQuery("select place_id, ST_disjoint(ST_GeometryFromText(place_pt), ST_GeometryFromText(place_pt)) disjoint from places where ST_disjoint(ST_GeometryFromText(place_pt), ST_GeometryFromText(place_pt)) = true order by place_id");
        assertQuery("select drainage_id, ST_disjoint(ST_GeometryFromText(stream_path),ST_GeometryFromText(stream_path)) \"disjoint same multiLine\" from drainage order by drainage_id");
        assertQuery("select department_id, ST_disjoint(ST_GeometryFromText(employee_loc),ST_GeometryFromText(employee_loc)) \"disjoint same multipoint\" from department order by department_id");
        assertQuery("select saleszone_id, ST_disjoint(ST_GeometryFromText(zone_area),ST_GeometryFromText(zone_area)) \"disjoint same poly\" from saleszones order by saleszone_id");
        // st_disjoint8.rxp
        assertQuery("select l1.id \"line1\", l2.id \"line2\", ST_disjoint(ST_GeometryFromText(l1.wkt), ST_GeometryFromText(l2.wkt)) \"disjoint(line1,line2)\", ST_disjoint(ST_GeometryFromText(l2.wkt), ST_GeometryFromText(l1.wkt)) \"disjoint(line2,line1)\" from lines l1, lines l2 where l1.id >= l2.id and l1.id < 600 order by l1.id, l2.id");
        assertQuery("select p1.id \"poly1\", p2.id \"poly2\", ST_disjoint(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)) \"disjoint(poly1,poly2)\", ST_disjoint(ST_GeometryFromText(p2.wkt), ST_GeometryFromText(p1.wkt)) \"disjoint(poly2,poly1)\" from polygons p1, polygons p2 where p1.id >= p2.id and p1.id < 1200 order by p1.id, p2.id");
        assertQuery("select p1.id \"poly\", p2.id \"point\", ST_disjoint(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)) \"disjoint(poly,point)\", ST_disjoint(ST_GeometryFromText(p2.wkt), ST_GeometryFromText(p1.wkt)) \"disjoint(point,poly)\" from polygons p1, points p2 where p1.id < 1004 and p2.id < 4 order by p1.id, p2.id");
    }

    @Test
    public void testStEquals()
    {
        // All tests fail because of mismatch with empty case. Implementation discussed in issue: https://github.com/prestodb/presto/issues/26253
        // st_equals.rxp
//        assertQuery("select road_id, ST_equals(ST_GeometryFromText(road), ST_GeometryFromText(road)) \"equals same line\" from roads order by road_id");
//        assertQuery("select county_id, ST_equals(ST_GeometryFromText(county), ST_GeometryFromText(county)) \"equals same poly\" from counties order by county_id");
//        assertQuery("select place_id, ST_equals(ST_GeometryFromText(place_pt), ST_GeometryFromText(place_pt)) \"equals same point\" from places order by place_id");
//        assertQuery("select place_id, county_id, ST_equals(ST_GeometryFromText(place_pt), ST_GeometryFromText(county)) \"equals(point,poly)\", ST_equals(ST_GeometryFromText(county),ST_GeometryFromText(place_pt)) \"equals(poly,point)\" from places, counties order by place_id, county_id");
//        assertQuery("select place_id, road_id, ST_equals(ST_GeometryFromText(place_pt), ST_GeometryFromText(road)) \"equals(point,line)\", ST_equals(ST_GeometryFromText(road),ST_GeometryFromText(place_pt)) \"equals(line,point)\" from roads, places order by place_id, road_id");
//        assertQuery("select county_id, road_id, ST_equals(ST_GeometryFromText(county), ST_GeometryFromText(road)) \"equals(poly,line)\", ST_equals(ST_GeometryFromText(road),ST_GeometryFromText(county)) \"equals(line,poly)\" from roads, counties order by county_id, road_id");
//        assertQuery("select county_id, road_id, ST_equals(ST_GeometryFromText(county), ST_GeometryFromText(road)) equals from roads, counties where ST_equals(ST_GeometryFromText(county), ST_GeometryFromText(road)) = true order by county_id, road_id");
//        assertQuery("select place_id, road_id,ST_equals(ST_GeometryFromText(place_pt), ST_GeometryFromText(road)) equals from roads, places where ST_equals(ST_GeometryFromText(place_pt), ST_GeometryFromText(road)) = true and ST_equals(ST_GeometryFromText(place_pt), ST_GeometryFromText(road)) = false order by place_id, road_id");
//        assertQuery("select place_id, ST_equals(ST_GeometryFromText(place_pt), ST_GeometryFromText(place_pt)) equals from places where ST_equals(ST_GeometryFromText(place_pt), ST_GeometryFromText(place_pt)) = true order by place_id");
//        assertQuery("select drainage_id, ST_equals(ST_GeometryFromText(stream_path),ST_GeometryFromText(stream_path)) \"equals same multiLine\" from drainage order by drainage_id");
//        assertQuery("select department_id, ST_equals(ST_GeometryFromText(employee_loc),ST_GeometryFromText(employee_loc)) \"equals same multipoint\" from department order by department_id");
//        assertQuery("select saleszone_id, ST_equals(ST_GeometryFromText(zone_area),ST_GeometryFromText(zone_area)) \"equals same poly\" from saleszones order by saleszone_id");
        // st_equals8.rxp
//        assertQuery("SELECT l1.id, l2.id, ST_Equals(ST_GeometryFromText(l1.wkt), ST_GeometryFromText(l2.wkt)), ST_Equals(ST_GeometryFromText(l2.wkt), ST_GeometryFromText(l1.wkt)) FROM lines AS l1, lines AS l2 WHERE  l1.id >= l2.id and l1.id < 600 AND l1.id <> 527 AND l2.id <> 527 ORDER BY l1.id, l2.id");
//        assertQuery("SELECT l1.id, l2.id, ST_Equals(ST_GeometryFromText(l1.wkt), ST_GeometryFromText(l2.wkt)), ST_Equals(ST_GeometryFromText(l2.wkt), ST_GeometryFromText(l1.wkt)) FROM lines AS l1, lines AS l2 WHERE  l1.id >= l2.id and l1.id < 600 ORDER BY l1.id, l2.id");
//        assertQuery("SELECT p1.id, p2.id, ST_equals(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)), ST_equals(ST_GeometryFromText(p2.wkt), ST_GeometryFromText(p1.wkt)) FROM polygons AS p1, polygons AS p2 WHERE p1.id >= p2.id AND p1.id < 1200 AND p1.id <> 1019 AND p2.id <> 1019 ORDER BY p1.id, p2.id");
//        assertQuery("select p1.id \"poly1 srid0\", p2.id \"poly2 srid1\", ST_equals(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)) \"equals(poly1,poly2)\", ST_equals(ST_GeometryFromText(p2.wkt), ST_GeometryFromText(p1.wkt)) \"equals(poly2,poly1)\" from polygons p1, polygons p2 where p1.id = 1001 and p2.id = 1001 order by p1.id, p2.id");
//        assertQuery("select p1.id \"poly1 srid1\", p2.id \"poly2 srid12\", ST_equals(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)) \"equals(poly1,poly2)\", ST_equals(ST_GeometryFromText(p2.wkt), ST_GeometryFromText(p1.wkt)) \"equals(poly2,poly1)\" from polygons p1, polygons p2 where p1.id = 1001 and p2.id = 1001 order by p1.id, p2.id");
//        assertQuery("select p1.id \"poly\", p2.id \"point\", ST_equals(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)) \"equals(poly,point)\", ST_equals(ST_GeometryFromText(p2.wkt), ST_GeometryFromText(p1.wkt)) \"equals(point,poly)\" from polygons p1, points p2 where p1.id < 1004 and p2.id < 4 order by p1.id, p2.id");
//        assertQuery("SELECT p1.id, p2.id FROM polygons p1, polygons p2 WHERE ST_equals(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)) = true AND p1.id <> 1019 AND p2.id <> 1019 ORDER BY p1.id, p2.id");
//        assertQuery("SELECT ST_Equals(ST_GeometryFromText(text1), ST_GeometryFromText(text2)) FROM ( VALUES ( 'linestring (1 1, 9 9)', 'linestring (1 1, 3 3, 5 5, 7 7, 9 9)' ), ( 'linestring zm (10 10 10 10, 20 20 20 20)', 'linestring zm (10 10 10 10, 15 15 15 15, 20 20 20 20)' ), ( 'linestring z (3 3 3, 5 5 5)', 'linestring (3 3, 5 5)' ), ( 'linestring z (3 3 3, 5 5 5)', 'linestring z (3 3 5, 5 5 5)' ), ( 'linestring (20 20, 40 40)', 'multilinestring ((20 20, 30 30),(30 30, 40 40))' ), ( 'polygon ((20 20, 20 40, 40 40, 40 20, 20 20), (25 25, 25 30, 30 30, 30 25, 25 25))', 'polygon ((20 20, 40 20, 40 40, 20 40, 20 20), (25 25, 30 25, 30 30, 25 30, 25 25))' ), ( 'polygon ((10 10, 20 10, 20 20, 10 20, 10 10))', 'polygon ((10 10, 15 10, 20 10, 20 15, 20 20, 15 20, 10 20, 10 15, 10 10))' ), ( 'polygon ((1 1, 1 10, 10 10, 10 1, 1 1))', 'multipolygon (((1 1, 1 10, 10 10, 10 1, 1 1)))' ), ( 'polygon ((1 1, 1 10, 10 10, 10 1, 1 1))', 'multipolygon (((1 1, 1 5, 1 10, 10 10, 10 1, 1 1)))' ) ) AS t (text1, text2)");
    }

    @Test
    public void testStOverlaps()
    {
        // st_overlaps.rxp
        assertQuery("select r1.road_id, r2.road_id, ST_overlaps(ST_GeometryFromText(r1.road), ST_GeometryFromText(r2.road)) \"overlaps(line1,line2)\" , ST_overlaps(ST_GeometryFromText(r2.road), ST_GeometryFromText(r1.road)) \"overlaps(line2,line1)\" from roads r1, roads r2 order by r1.road_id, r2.road_id");
        assertQuery("select c1.county_id, c2.county_id, ST_overlaps(ST_GeometryFromText(c1.county), ST_GeometryFromText(c2.county)) \"overlaps(poly1,poly2)\", ST_overlaps(ST_GeometryFromText(c2.county), ST_GeometryFromText(c1.county)) \"overlaps(poly2,poly1)\" from counties c1, counties c2 order by c1.county_id, c2.county_id");
        assertQuery("select  place_id, county_id, ST_overlaps(ST_GeometryFromText(place_pt), ST_GeometryFromText(county)) \"overlaps(point,poly)\", ST_overlaps(ST_GeometryFromText(county),ST_GeometryFromText(place_pt)) \"overlaps(poly,point)\" from places, counties order by place_id, county_id");
        assertQuery("select place_id,road_id, ST_overlaps(ST_GeometryFromText(place_pt), ST_GeometryFromText(road)) \"overlaps(point,line)\", ST_overlaps(ST_GeometryFromText(road),ST_GeometryFromText(place_pt)) \"overlaps(line,point)\" from roads, places order by place_id, road_id");
        assertQuery("select county_id,road_id, ST_overlaps(ST_GeometryFromText(county), ST_GeometryFromText(road)) \"overlaps(poly,line)\", ST_overlaps(ST_GeometryFromText(road),ST_GeometryFromText(county)) \"overlaps(line,poly)\" from roads, counties order by county_id, road_id");
        assertQuery("select county_id, road_id, ST_overlaps(ST_GeometryFromText(county), ST_GeometryFromText(road)) \"overlaps(poly,line)\" from roads, counties where ST_overlaps(ST_GeometryFromText(county), ST_GeometryFromText(road)) = false order by county_id, road_id");
        assertQuery("select r1.road_id, r2.road_id, ST_overlaps(ST_GeometryFromText(r1.road), ST_GeometryFromText(r2.road)) \"overlaps(line,line)\" from roads r1, roads r2 where ST_overlaps(ST_GeometryFromText(r1.road), ST_GeometryFromText(r2.road)) = true order by r1.road_id,  r2.road_id");
        assertQuery("select c1.county_id, c2.county_id, ST_overlaps(ST_GeometryFromText(c1.county), ST_GeometryFromText(c2.county)) \"overlaps(poly,poly)\" from counties c1, counties c2 where ST_overlaps(ST_GeometryFromText(c1.county), ST_GeometryFromText(c2.county)) = true order by c1.county_id,  c2.county_id");
        assertQuery("select saleszone_id, road_id, ST_overlaps(ST_GeometryFromText(zone_area), ST_GeometryFromText(road)) \"overlaps(multipoly,line)\" from saleszones, roads order by saleszone_id, road_id");
        assertQuery("select drainage_id,county_id, ST_overlaps(ST_GeometryFromText(stream_path), ST_GeometryFromText(county)) \"overlaps(multiline,poly)\" from drainage, counties order by drainage_id, county_id");
        assertQuery("select department_id, county_id, ST_overlaps(ST_GeometryFromText(employee_loc), ST_GeometryFromText(county)) \"overlaps(multipoint,poly)\" from department, counties order by department_id, county_id");
        // st_overlaps8.rxp
        assertQuery("select l1.id \"line1\", l2.id \"line2\", ST_overlaps(ST_GeometryFromText(l1.wkt), ST_GeometryFromText(l2.wkt)) \"overlaps(line1,line2)\", ST_overlaps(ST_GeometryFromText(l2.wkt), ST_GeometryFromText(l1.wkt)) \"overlaps(line2,line1)\" from lines l1, lines l2 where l1.id >= l2.id and l1.id < 600 order by l1.id, l2.id");
        assertQuery("select p1.id \"poly1\", p2.id \"poly2\", ST_overlaps(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)) \"overlaps(poly1,poly2)\", ST_overlaps(ST_GeometryFromText(p2.wkt), ST_GeometryFromText(p1.wkt)) \"overlaps(poly2,poly1)\" from polygons p1, polygons p2 where p1.id >= p2.id and p1.id < 1200 order by p1.id, p2.id");
        assertQuery("select p1.id \"poly\", p2.id \"point\", ST_overlaps(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)) \"overlaps(poly,point)\", ST_overlaps(ST_GeometryFromText(p2.wkt), ST_GeometryFromText(p1.wkt)) \"overlaps(point,poly)\" from polygons p1, points p2 where p1.id < 1004 and p2.id < 4 order by p1.id, p2.id");
    }

    @Test
    public void testStRelate()
    {
        // st_relate.rxp
        // st_equals failing because of the empty case mismatch.
//        assertQuery("select road_id, ST_relate(ST_GeometryFromText(road), ST_GeometryFromText(road), 'T*F**FFF*') \"relate equals\", ST_equals(ST_GeometryFromText(road),ST_GeometryFromText(road)) \"equals\" from roads order by road_id");
//        assertQuery("select county_id, ST_relate(ST_GeometryFromText(county), ST_GeometryFromText(county), 'T*F**FFF*') \"relate equals\", ST_equals(ST_GeometryFromText(county),ST_GeometryFromText(county)) \"equals\" from counties order by county_id");
//        assertQuery("select place_id, ST_relate(ST_GeometryFromText(place_pt), ST_GeometryFromText(place_pt), 'T*F**FFF*') \"relate equals\", ST_equals(ST_GeometryFromText(place_pt), ST_GeometryFromText(place_pt)) \"equals\" from places order by place_id");
        assertQuery("select place_id, county_id, ST_relate(ST_GeometryFromText(place_pt), ST_GeometryFromText(county), 'T*F**F***') \"relate within\", ST_within(ST_GeometryFromText(place_pt), ST_GeometryFromText(county)) \"within\" from places, counties order by place_id, county_id");
        assertQuery("select place_id, road_id, ST_relate(ST_GeometryFromText(place_pt), ST_GeometryFromText(road), 'T*F**F***') \"relate within\", ST_within(ST_GeometryFromText(place_pt), ST_GeometryFromText(road)) \"within\" from roads, places order by place_id, road_id");
        assertQuery("select road_id,county_id, ST_relate(ST_GeometryFromText(road),ST_GeometryFromText(county), 'T*F**F***') \"relate within\", ST_within(ST_GeometryFromText(road),ST_GeometryFromText(county)) \"within\" from roads, counties order by road_id, county_id");
        assertQuery("select road_id, county_id, ST_relate(ST_GeometryFromText(road),ST_GeometryFromText(county), 'T*F**F***') \"relate within\", ST_within(ST_GeometryFromText(road),ST_GeometryFromText(county)) \"within\" from roads, counties where ST_relate(ST_GeometryFromText(road),ST_GeometryFromText(county), 'T*F**F***') = true order by road_id, county_id");
        assertQuery("select road_id, county_id, ST_relate(ST_GeometryFromText(road),ST_GeometryFromText(county), 'T*F**F***') \"relate within\", ST_within(ST_GeometryFromText(road),ST_GeometryFromText(county)) \"within\" from roads, counties where ST_relate(ST_GeometryFromText(road),ST_GeometryFromText(county), 'T*F**F***') = false order by road_id, county_id");
        assertQuery("select place_id, road_id, ST_relate(ST_GeometryFromText(place_pt), ST_GeometryFromText(road), 'T*F**F***') \"relate within\", ST_within(ST_GeometryFromText(place_pt),ST_GeometryFromText(road)) \"within\" from roads, places where ST_relate(ST_GeometryFromText(place_pt), ST_GeometryFromText(road), 'T*F**F***') = true order by place_id, road_id");
        assertQuery("select road_id,saleszone_id, ST_relate(ST_GeometryFromText(road), ST_GeometryFromText(zone_area), 'T*F**F***') \"relate within\", ST_within(ST_GeometryFromText(road),ST_GeometryFromText(zone_area)) \"within\" from saleszones, roads order by road_id, saleszone_id");
        assertQuery("select  drainage_id,county_id, ST_relate(ST_GeometryFromText(stream_path), ST_GeometryFromText(county), 'T*F**F***') \"relate within\", ST_within(ST_GeometryFromText(stream_path),ST_GeometryFromText(county)) \"within\" from drainage, counties order by drainage_id, county_id");
        assertQuery("select department_id,county_id, ST_relate(ST_GeometryFromText(employee_loc), ST_GeometryFromText(county), 'T*F**F***') \"relate within\", ST_within(ST_GeometryFromText(employee_loc),ST_GeometryFromText(county)) \"within\" from department, counties order by department_id, county_id");
        // st_relate8.rxp
        // st_equals failing because of the empty case mismatch.
//        assertQuery("select a.id, b.id, ST_relate(ST_GeometryFromText(a.wkt), ST_GeometryFromText(b.wkt), 'T*F**FFF*') \"relate equals T*F**FFF*\", ST_relate(ST_GeometryFromText(a.wkt), ST_GeometryFromText(b.wkt), 'T*F**FFFT') \"relate equals T*F**FFFT\", ST_equals(ST_GeometryFromText(a.wkt), ST_GeometryFromText(b.wkt)) \"equals\" from lines a, lines b where a.id >= b.id order by a.id, b.id");
        assertQuery("select pt.id, p.id, ST_relate(ST_GeometryFromText(pt.wkt), ST_GeometryFromText(p.wkt), 'T*F**F***') \"relate within\", ST_within(ST_GeometryFromText(pt.wkt), ST_GeometryFromText(p.wkt)) \"within\" from points pt, points p where pt.id < 4 and p.id < 1004 order by  pt.id, p.id");
        assertQuery("select a.id, b.id from polygons a, polygons b where ST_relate(ST_GeometryFromText(a.wkt), ST_GeometryFromText(b.wkt), 'T*F**FFF*') = true order by a.id, b.id");
        assertQuery("select a.id, b.id from points a, polygons b where ST_relate(ST_GeometryFromText(a.wkt), ST_GeometryFromText(b.wkt), '0*F**FFF2') = true order by a.id, b.id");
        assertQuery("select a.id, b.id from lines a, lines b where ST_relate(ST_GeometryFromText(a.wkt), ST_GeometryFromText(b.wkt), '0F1***1F2') = true order by a.id, b.id");
        assertQuery("select a.id, b.id from polygons a, lines b where ST_relate(ST_GeometryFromText(a.wkt), ST_GeometryFromText(b.wkt), '11*******') = true order by a.id, b.id");
        assertQueryError("select a.id, b.id from polygons a, polygons b where ST_relate(ST_GeometryFromText(a.wkt), ST_GeometryFromText(b.wkt), '123456789') = true order by a.id, b.id", "Geometry Exception: relation string");
        assertQueryFails("select a.id, b.id from polygons a, polygons b where ST_relate(ST_GeometryFromText(a.wkt), ST_GeometryFromText(b.wkt), 'XYZ') = true order by a.id, b.id", " Failed to check geometry relation: IllegalArgumentException: IllegalArgumentException: Should be length 9, is [XYZ] instead\n" +
                " presto.default.st_relate(presto.default.st_geometryfromtext(wkt), presto.default.st_geometryfromtext(wkt_1), XYZ:VARCHAR) Top-level Expression: presto.default.eq(presto.default.st_relate(presto.default.st_geometryfromtext(wkt), presto.default.st_geometryfromtext(wkt_1), XYZ:VARCHAR), true:BOOLEAN)");
        assertQuery("values ST_Relate(ST_GeometryFromText('multilinestring z((1 1 1, 2 2 2))'),ST_GeometryFromText('multipoint z(3 3 3)'), '0*TFT10T2')");
        assertQuery("values ST_Relate(ST_GeometryFromText('linestring (10 10, 15 15, 20 15, 25 10)'),ST_GeometryFromText('linestring (10 20, 15 15, 20 15, 20 20)'), '1*1***1*2')");
        assertQuery("values ST_Relate(ST_GeometryFromText('linestring (10 10, 15 15, 20 15, 25 10)'),ST_GeometryFromText('linestring (10 20, 15 15, 17 20, 20 15, 20 20)'), '0*1******')");
    }

    @Test
    public void testStTouches()
    {
        // st_touches.rxp
        assertQuery("select r1.road_id, r2.road_id, ST_touches(ST_GeometryFromText(r1.road), ST_GeometryFromText(r2.road)) \"touches(line1,line2)\" , ST_touches(ST_GeometryFromText(r2.road), ST_GeometryFromText(r1.road)) \"touches(line2,line1)\" from roads r1, roads r2 order by r1.road_id,r2.road_id");
        assertQuery("select c1.county_id, c2.county_id, ST_touches(ST_GeometryFromText(c1.county), ST_GeometryFromText(c2.county)) \"touches(poly1,poly2)\", ST_touches(ST_GeometryFromText(c2.county), ST_GeometryFromText(c1.county)) \"touches(poly2,poly1)\" from counties c1, counties c2 order by c1.county_id, c2.county_id");
        assertQuery("select  place_id, county_id, ST_touches(ST_GeometryFromText(place_pt), ST_GeometryFromText(county)) \"touches(point,poly)\", ST_touches(ST_GeometryFromText(county),ST_GeometryFromText(place_pt)) \"touches(poly,point)\" from places, counties order by place_id, county_id");
        assertQuery("select place_id,road_id, ST_touches(ST_GeometryFromText(place_pt), ST_GeometryFromText(road)) \"touches(point,line)\", ST_touches(ST_GeometryFromText(road),ST_GeometryFromText(place_pt)) \"touches(line,point)\" from roads, places order by place_id,road_id");
        assertQuery("select county_id,road_id, ST_touches(ST_GeometryFromText(county), ST_GeometryFromText(road)) \"touches(poly,line)\", ST_touches(ST_GeometryFromText(road),ST_GeometryFromText(county)) \"touches(line,poly)\" from roads, counties order by county_id,road_id");
        assertQuery("select county_id, road_id, ST_touches(ST_GeometryFromText(county), ST_GeometryFromText(road)) \"touches(poly,line)\" from roads, counties where ST_touches(ST_GeometryFromText(county), ST_GeometryFromText(road)) = false order by county_id, road_id");
        assertQuery("select r1.road_id, r2.road_id, ST_touches(ST_GeometryFromText(r1.road), ST_GeometryFromText(r2.road)) \"touches(line,line)\" from roads r1, roads r2 where ST_touches(ST_GeometryFromText(r1.road), ST_GeometryFromText(r2.road)) = true order by r1.road_id, r2.road_id");
        assertQuery("select c1.county_id, c2.county_id, ST_touches(ST_GeometryFromText(c1.county), ST_GeometryFromText(c2.county)) \"touches(poly,poly)\" from counties c1, counties c2 where ST_touches(ST_GeometryFromText(c1.county), ST_GeometryFromText(c2.county)) = true order by c1.county_id, c2.county_id");
        assertQuery("select saleszone_id, road_id, ST_touches(ST_GeometryFromText(zone_area), ST_GeometryFromText(road)) \"touches(multipoly,line)\" from saleszones, roads order by saleszone_id,road_id");
        assertQuery("select drainage_id,county_id, ST_touches(ST_GeometryFromText(stream_path), ST_GeometryFromText(county)) \"touches(multiline,poly)\" from drainage, counties order by drainage_id,county_id");
        assertQuery("select department_id, county_id, ST_touches(ST_GeometryFromText(employee_loc), ST_GeometryFromText(county)) \"touches(multipoint,poly)\" from department, counties order by department_id, county_id");
        // st_touches8.rxp
        assertQuery("select l1.id \"line1\", l2.id \"line2\", ST_touches(ST_GeometryFromText(l1.wkt), ST_GeometryFromText(l2.wkt)) \"touches(line1,line2)\", ST_touches(ST_GeometryFromText(l2.wkt), ST_GeometryFromText(l1.wkt)) \"touches(line2,line1)\" from lines l1, lines l2 where l1.id >= l2.id and l1.id < 600 order by l1.id, l2.id");
        assertQuery("select p1.id \"poly1\", p2.id \"poly2\", ST_touches(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)) \"touches(poly1,poly2)\", ST_touches(ST_GeometryFromText(p2.wkt), ST_GeometryFromText(p1.wkt)) \"touches(poly2,poly1)\" from polygons p1, polygons p2 where p1.id >= p2.id and p1.id < 1200 order by p1.id, p2.id");
        assertQuery("select p1.id \"poly\", p2.id \"point\", ST_touches(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)) \"touches(poly,point)\", ST_touches(ST_GeometryFromText(p2.wkt), ST_GeometryFromText(p1.wkt)) \"touches(point,poly)\" from polygons p1, points p2 where p1.id < 1004 and p2.id < 4 order by p1.id, p2.id");
    }

    @Test
    public void testStBoundary()
    {
        // COMMENTED OUT TESTS BECAUSE OF MULTIPOINT PARENTHESIS MISMATCH or MULTILINESTRING values in different order not necessarily wrong...
        // ST_Boundary01.rxp
        assertQuery("SELECT place_ID,cast(ST_AsText(ST_Boundary(ST_GeometryFromText(place_pt))) as varchar(500)) \"The Boundary\" FROM PLACES order by place_ID");
//        assertQuery("SELECT road_ID,cast(ST_AsText(ST_Boundary(ST_GeometryFromText(road))) as varchar(500)) \"The Boundary\" FROM ROADS order by road_ID");
        assertQuery("SELECT county_ID, cast(ST_AsText(ST_Boundary(ST_GeometryFromText(county))) as varchar(500)) \"The Boundary\" FROM COUNTIES order by county_ID");
        assertQuery("SELECT p1.place_ID \"p1_ID\", p2.place_ID \"p2_ID\", cast(ST_AsText(ST_Boundary(ST_Union(ST_GeometryFromText(p1.place_pt),ST_GeometryFromText(p2.place_pt)))) as varchar(500)) \"MutiPoint Boundary\" FROM PLACES as p1, PLACES as p2 order by p1.place_ID, p2.place_ID");
//        assertQuery("SELECT r1.road_ID \"r1_ID\", r2.road_ID \"r2_ID\", cast(ST_AsText(ST_Boundary(ST_Union(ST_GeometryFromText(r1.road),ST_GeometryFromText(r2.road)))) as varchar(500)) \"MultiLine Boundary\" FROM ROADS as r1, ROADS as r2 order by r1.road_ID, r2.road_ID");
//        assertQuery("SELECT c1.county_ID \"c1_ID\", c2.county_ID \"c2_ID\", cast(ST_AsText(ST_Boundary(ST_Union(ST_GeometryFromText(c1.county),ST_GeometryFromText(c2.county)))) as varchar(500)) \"MultiPolygon Boundary\" FROM COUNTIES as c1, COUNTIES as c2 order by c1.county_ID, c2.county_ID");
        assertQuery("SELECT drainage_ID, ST_GeometryType(ST_GeometryFromText(stream_path)) \"GeometryType\", cast(ST_AsText(ST_GeometryFromText(stream_path)) as varchar(500)) \"Boundary\" FROM DRAINAGE order by drainage_ID");
//        assertQuery("SELECT drainage_ID, ST_GeometryType(ST_GeometryFromText(stream_path)) \"GeometryType\", cast(ST_AsText(ST_Boundary(ST_GeometryFromText(stream_path))) as varchar(500)) \"Boundary\" FROM DRAINAGE order by drainage_ID");
        assertQuery("SELECT department_ID, ST_GeometryType(ST_GeometryFromText(employee_loc)) \"GeometryType\", cast(ST_AsText(ST_Boundary(ST_GeometryFromText(employee_loc))) as varchar(500)) \"Boundary\" FROM DEPARTMENT order by department_ID");
        assertQuery("SELECT saleszone_ID, ST_GeometryType(ST_GeometryFromText(zone_area)) \"GeometryType\", cast(ST_AsText(ST_Boundary(ST_GeometryFromText(zone_area))) as varchar(500)) \"Boundary\" FROM SALESZONES order by saleszone_ID");
//        assertQuery("SELECT everyplace_ID, ST_GeometryType(ST_GeometryFromText(everyplace_geometry)) \"GeometryType\", cast(ST_AsText(ST_Boundary(ST_GeometryFromText(everyplace_geometry))) as varchar(500)) \"Boundary\" FROM EVERYPLACE order by everyplace_ID");
//        assertQuery("SELECT collection_ID, ST_GeometryType(ST_GeometryFromText(collection_multi)) \"GeometryType\", cast(ST_AsText(ST_Boundary(ST_GeometryFromText(collection_multi))) as varchar(500)) \"Boundary\" FROM COLLECTION order by collection_ID");
        // ST_Boundary02.rxp
//        assertQuery("SELECT geoID,geoType, cast(ST_AsText(ST_Boundary(ST_GeometryFromText(wkt))) as varchar(280)) \"The Boundary\" FROM GEOMETRY_TEST order by geoID,geoType");
//        assertQuery("SELECT geoID, ST_GeometryType(ST_GeometryFromText(wkt)) \"Type of Boundary\", cast(ST_AsText(ST_GeometryFromText(wkt)) as varchar(280)) \"The Boundary\" FROM GEOMETRY_TEST order by geoID");
        // ST_Boundary03.rxp
        assertQueryFails("SELECT geoType,cast(ST_AsText(ST_Boundary(13)) as varchar(280)) \"The Boundary\" FROM GEOMETRY_TEST WHERE geoID = 1", "line 1:31: Unexpected parameters (integer) for function st_boundary. Expected: st_boundary(Geometry) ");
        assertQuery("SELECT geoType,cast(ST_AsText(ST_Boundary(NULL)) as varchar(280)) \"The Boundary\" FROM GEOMETRY_TEST WHERE geoID = 1");
        assertQueryFails("SELECT geoType,cast(ST_AsText(ST_Boundary(ST_GeometryFromText('error'))) as varchar(280)) \"The Boundary\" FROM GEOMETRY_TEST WHERE geoID = 1", "Invalid WKT: Expected word but found End-of-Stream (line 1)");
        // ST_Boundary08.rxp
        assertQuery("SELECT id,cast(ST_AsText(ST_Boundary(ST_GeometryFromText(wkt))) as varchar(1000)) \"The Boundary\" FROM polygons where id <> 1018 order by id");
        assertQuery("SELECT id,cast(ST_AsText(ST_Boundary(ST_GeometryFromText(wkt))) as varchar(1000)) \"The Boundary\" FROM multipolygons where id <> 2804 order by id");
//        assertQuery("SELECT id,cast(ST_AsText(ST_Boundary(ST_GeometryFromText(wkt))) as varchar(1000)) \"The Boundary\" FROM collections where id <> 2804 order by id");
//        assertQuery("SELECT id,cast(ST_AsText(ST_Boundary(ST_GeometryFromText(wkt))) as varchar(1000)) \"The Boundary\" FROM curves order by id");
        assertQuery("SELECT id,cast(ST_AsText(ST_Boundary(ST_GeometryFromText(wkt))) as varchar(1000)) \"The Boundary\" FROM surfaces where id <> 1018 order by id");
    }

    @Test
    public void testStDifference()
    {
        // ST_Difference.rxp
        // mismatch between libraries for empty polygon OGCGeometry is returning multipolygon since the ring count = 0 instead of 1.
        // but on C++ it returns just normal polygon type. example: [1, 1, MULTIPOLYGON EMPTY] vs [1, 1, POLYGON EMPTY]
//        assertQuery("SELECT T1.county_id,T2.county_id,cast(ST_AsText(ST_Difference(ST_GeometryFromText(T1.county),ST_GeometryFromText(T2.county))) as varchar(512)) \"Location\" FROM COUNTIES AS T1,COUNTIES AS T2 order by T1.county_id, T2.county_id");
//        assertQuery("SELECT T1.road_id,T2.road_id,cast(ST_AsText(ST_Difference(ST_GeometryFromText(T1.road),ST_GeometryFromText(T2.road))) as varchar (512)) \"Location\" FROM ROADS AS T1,ROADS AS T2 order by T1.road_id, T2.road_id");
        assertQuery("SELECT T1.place_id,T2.place_id,cast(ST_AsText(ST_Difference(ST_GeometryFromText(T1.place_pt),ST_GeometryFromText(T2.place_pt))) as varchar (128)) \"Location\" FROM PLACES AS T1,PLACES AS T2 order by T1.place_id,T2.place_id");
//        assertQuery("SELECT T1.saleszone_id,T2.county_id,cast(ST_AsText(ST_Difference(ST_GeometryFromText(T1.zone_area),ST_GeometryFromText(T2.county))) as varchar(512)) \"Location\" FROM SALESZONES T1,COUNTIES T2 order by T1.saleszone_id, T2.county_id");
//        assertQuery("SELECT T1.drainage_id,T2.road_id,cast(ST_AsText(ST_Difference(ST_GeometryFromText(T1.stream_path),ST_GeometryFromText(T2.road))) as varchar (256)) \"Location\" FROM DRAINAGE T1,ROADS T2 order by T1.drainage_id, T2.road_id");
//        assertQuery("SELECT T1.department_id,T2.place_id,cast(ST_AsText(ST_Difference(ST_GeometryFromText(T1.employee_loc),ST_GeometryFromText(T2.place_pt))) as varchar (256)) \"Location\" FROM DEPARTMENT T1,PLACES T2 order by T1.department_id, T2.place_id");
        // Order of Polygon numbers is different not necessarily wrong
//        assertQuery("SELECT T2.county_id,T1.saleszone_id,cast(ST_AsText(ST_Difference(ST_GeometryFromText(T2.county),ST_GeometryFromText(T1.zone_area))) as varchar(512)) \"Location\" FROM SALESZONES T1, COUNTIES T2 order by T2.county_id,T1.saleszone_id");
        assertQuery("select count(*) from (select t2.road_id, t1.drainage_id from drainage t1, roads t2 where ST_XMax(ST_GeometryFromText(t1.stream_path)) < 58.9 order by t2.road_id, t1.drainage_id)");
        // Multilinestring vs linestring problems where internal libraries mismatch with one returning multilinestring empty and other returns linestrimg empty...
//        assertQuery("SELECT t2.road_id, t1.drainage_id, cast(ST_AsText(ST_Difference(ST_GeometryFromText(t2.road), ST_GeometryFromText(t1.stream_path))) as varchar (256)) FROM drainage t1, roads t2 WHERE ST_XMax(ST_GeometryFromText(t1.stream_path)) < 58.9 ORDER BY t2.road_id, t1.drainage_id");
        assertQuery("SELECT T2.place_id,T1.department_id,cast(ST_AsText(ST_Difference(ST_GeometryFromText(T2.place_pt),ST_GeometryFromText(T1.employee_loc))) as varchar (256)) \"Location\" FROM DEPARTMENT T1,PLACES T2 order by T2.place_id, T1.department_id");
//        assertQuery("SELECT T1.saleszone_id,T2.saleszone_id,cast(ST_AsText(ST_Difference(ST_GeometryFromText(T1.zone_area),ST_GeometryFromText(T2.zone_area))) as varchar(512)) \"Location\" FROM SALESZONES T1,SALESZONES T2 order by T1.saleszone_id,T2.saleszone_id");
//        assertQuery("SELECT T1.drainage_id,T2.drainage_id,cast(ST_AsText(ST_Difference(ST_GeometryFromText(T1.stream_path),ST_GeometryFromText(T2.stream_path))) as varchar (256)) \"Location\" FROM DRAINAGE T1,DRAINAGE T2  order by T1.drainage_id, T2.drainage_id");
//        assertQuery("SELECT T1.department_id,T2.department_id,cast(ST_AsText(ST_Difference(ST_GeometryFromText(T1.employee_loc),ST_GeometryFromText(T2.employee_loc))) as varchar (256)) \"Location\" FROM DEPARTMENT T1,DEPARTMENT T2 order by T1.department_id, T2.department_id");
        // ST_Difference8.rxp
        assertQuery("SELECT a.id, b.id, cast(ST_AsText(ST_Difference(ST_GeometryFromText(a.wkt),ST_GeometryFromText(b.wkt))) as varchar(512)) \"Difference Point/Point\" FROM points a, points b where a.id< 4 and b.id < 4 order by a.id, b.id");
//        assertQuery("SELECT a.id, b.id, cast(ST_AsText(ST_Difference(ST_GeometryFromText(a.wkt),ST_GeometryFromText(b.wkt))) as varchar(512)) \"Difference Poly/Poly\" FROM polygons a, polygons b where a.id< 1004 and b.id < 1004 order by a.id, b.id");
//        assertQuery("SELECT a.id, b.id, cast(ST_AsText(ST_Difference(ST_GeometryFromText(a.wkt),ST_GeometryFromText(b.wkt))) as varchar(512)) \"Difference MPoly/Poly\" FROM multipolygons a, polygons b where a.id< 2504 and b.id < 1004 order by a.id, b.id");
    }

    @Test
    public void testStEnvelope()
    {
        // st_envelope8.rxp
        assertQuery("select id, cast(ST_astext(ST_envelope(ST_GeometryFromText(wkt))) as varchar(200) ) from polygons order by id");
        // For straight lines c++ throws error saying geometry is empty but java is less strict and allows a polygon with 0 width to be formed...
//        assertQuery("select id, cast(ST_astext(ST_envelope(ST_GeometryFromText(wkt))) as varchar(200) ) from collections where id < 1804 order by id");
//        assertQuery("select id, cast(ST_astext(ST_envelope(ST_GeometryFromText(wkt))) as varchar(200) ) from lines order by id");
    }

    @Test
    public void testStExteriorRing()
    {
        // ST_ExteriorRing01.rxp
        assertQuery("SELECT county_ID, cast(ST_AsText(ST_ExteriorRing(ST_GeometryFromText(county))) as varchar(500)) \"Exterior Ring\" FROM COUNTIES ORDER BY county_ID");
        // ST_ExteriorRing03.rxp
        assertQueryFails("SELECT place_ID, cast(ST_AsText(ST_ExteriorRing(ST_GeometryFromText(place))) as varchar(500)) \"Exterior Ring\" FROM PLACES", "line 1:49: Unexpected parameters (integer) for function st_geometryfromtext. Expected: st_geometryfromtext(varchar) ");
        // ST_ExteriorRing08.rxp
        assertQuery("SELECT ID, cast(ST_AsText(ST_ExteriorRing(ST_GeometryFromText(wkt))) as varchar(500)) \"Exterior Ring\" FROM polygons order by id");
    }

    @Test
    public void testStIsValid()
    {
        // ST_IsValid01.rxp
        assertQuery("SELECT place_ID,ST_IsValid(ST_GeometryFromText(place_pt)) \"Is it valid?\" FROM PLACES order by place_ID");
        assertQuery("SELECT road_ID,ST_IsValid(ST_GeometryFromText(road))  \"Is it valid?\" FROM ROADS order by road_ID");
        assertQuery("SELECT county_ID,ST_IsValid(ST_GeometryFromText(county))  \"Is it valid?\" FROM COUNTIES order by county_ID");
        assertQuery("SELECT ST_IsValid(ST_GeometryFromText(p1.place_pt)) \"Is p1 Valid?\", ST_IsValid(ST_GeometryFromText(p2.place_pt)) \"Is p2 Valid?\", ST_IsValid(ST_Union(ST_GeometryFromText(p1.place_pt),ST_GeometryFromText(p2.place_pt))) \"Is Union Valid?\" FROM PLACES as p1, PLACES as p2 order by  1, 2");
        assertQuery("SELECT ST_IsValid(ST_GeometryFromText(r1.road)) \"Is r1 Valid?\", ST_IsValid(ST_GeometryFromText(r2.road)) \"Is r2 Valid?\", ST_IsValid(ST_Union(ST_GeometryFromText(r1.road),ST_GeometryFromText(r2.road))) \"Is Union Valid?\" FROM ROADS as r1, ROADS as r2 order by  1, 2");
        assertQuery("SELECT ST_IsValid(ST_GeometryFromText(c1.county)) \"Is c1 Valid?\", ST_IsValid(ST_GeometryFromText(c2.county)) \"Is c2 Valid?\", ST_IsValid(ST_Union(ST_GeometryFromText(c1.county),ST_GeometryFromText(c2.county))) \"Is Union Valid?\" FROM COUNTIES as c1, COUNTIES as c2 order by  1, 2");
        assertQuery("SELECT drainage_ID, ST_IsValid(ST_GeometryFromText(stream_path)) \"Is Valid?\" FROM DRAINAGE order by drainage_ID");
        assertQuery("SELECT department_ID, ST_IsValid(ST_GeometryFromText(employee_loc)) \"Is Valid?\" FROM DEPARTMENT order by department_ID");
        assertQuery("SELECT saleszone_ID, ST_IsValid(ST_GeometryFromText(zone_area)) \"Is Valid?\" FROM SALESZONES order by saleszone_ID");
        assertQuery("SELECT everyplace_ID, ST_IsValid(ST_GeometryFromText(everyplace_geometry)) \"Is Valid?\" FROM EVERYPLACE order by everyplace_ID");
        assertQuery("SELECT collection_ID, ST_IsValid(ST_GeometryFromText(collection_multi)) \"Is Valid?\" FROM COLLECTION order by collection_ID");
        // ST_IsValid02.rxp
        assertQuery("SELECT geoID,geoType,ST_IsValid(ST_GeometryFromText(wkt)) \"Is it valid?\" FROM GEOMETRY_TEST order by geoID,geoType");
        assertQuery("SELECT geoID,geoType,ST_IsValid(ST_GeometryFromText(wkt)) \"Should be 1\" FROM GEOMETRY_TEST WHERE ST_IsValid(ST_GeometryFromText(wkt)) <> false order by geoID,geoType");
        // ST_IsValid03.rxp
        assertQueryFails("SELECT geoID,geoType,ST_IsValid(13) \"Is it valid?\" FROM GEOMETRY_TEST WHERE  geoID = 1", "line 1:22: Unexpected parameters (integer) for function st_isvalid. Expected: st_isvalid(Geometry) ");
        assertQuery("SELECT geoID,geoType,ST_IsValid(NULL) \"Is it valid?\" FROM GEOMETRY_TEST WHERE  geoID = 1");
        assertQueryFails("SELECT geoID,geoType,ST_IsValid('error') \"Is it valid?\" FROM GEOMETRY_TEST WHERE  geoID = 1", "line 1:22: Unexpected parameters (varchar(5)) for function st_isvalid. Expected: st_isvalid(Geometry) ");
        // ST_IsValid08.rxp
        assertQuery("SELECT id,ST_IsValid(ST_GeometryFromText(wkt)) \"Is it valid?\" FROM geometries order by id");
        assertQuery("SELECT id,ST_IsValid(ST_GeometryFromText(wkt)) \"Is it valid?\" FROM points order by id");
    }

    @Test
    public void testStIsSimple()
    {
        // ST_IsSimple01.rxp
        assertQuery("SELECT place_ID,ST_IsSimple(ST_GeometryFromText(place_pt)) \"Is it simple?\" FROM PLACES order by place_ID");
        assertQuery("SELECT road_ID,ST_IsSimple(ST_GeometryFromText(road))  \"Is it simple?\" FROM ROADS order by road_ID");
        assertQuery("SELECT county_ID,ST_IsSimple(ST_GeometryFromText(county))  \"Is it simple?\" FROM COUNTIES order by county_ID");
        assertQuery("SELECT p1.place_ID \"p1_ID\", ST_IsSimple(ST_GeometryFromText(p1.place_pt)) \"Is p1 Simple?\", p2.place_ID \"p2_ID\", ST_IsSimple(ST_GeometryFromText(p2.place_pt)) \"Is p2 Simple?\", ST_IsSimple(ST_Union(ST_GeometryFromText(p1.place_pt),ST_GeometryFromText(p2.place_pt))) \"Is Union Simple?\" FROM PLACES as p1, PLACES as p2 WHERE p1.place_ID = p2.place_ID order by 1, 2");
        assertQuery("SELECT r1.road_ID \"r1_ID\", ST_IsSimple(ST_GeometryFromText(r1.road)) \"Is r1 Simple?\", r2.road_ID \"r2_ID\", ST_IsSimple(ST_GeometryFromText(r2.road)) \"Is r2 Simple?\", ST_IsSimple(ST_Union(ST_GeometryFromText(r1.road),ST_GeometryFromText(r2.road))) \"Is Union Simple?\" FROM ROADS as r1,ROADS as r2 WHERE  r1.road_ID = r2.road_ID order by 1, 2");
        assertQuery("SELECT c1.county_ID \"c1_ID\", ST_IsSimple(ST_GeometryFromText(c1.county)) \"Is c1 Simple?\", c2.county_ID \"c2_ID\", ST_IsSimple(ST_GeometryFromText(c2.county)) \"Is c2 Simple?\", ST_IsSimple(ST_Union(ST_GeometryFromText(c1.county),ST_GeometryFromText(c2.county))) \"Is Union Simple?\" FROM COUNTIES as c1, COUNTIES as c2 WHERE  c1.county_ID = c2.county_ID order by 1, 2");
        assertQuery("SELECT drainage_ID, ST_IsSimple(ST_GeometryFromText(stream_path)) \"Is Simple?\" FROM DRAINAGE order by drainage_ID");
        assertQuery("SELECT department_ID, ST_IsSimple(ST_GeometryFromText(employee_loc)) \"Is Simple?\" FROM DEPARTMENT order by department_ID");
        assertQuery("SELECT saleszone_ID, ST_IsSimple(ST_GeometryFromText(zone_area)) \"Is Simple?\" FROM SALESZONES order by saleszone_ID");
        assertQuery("SELECT everyplace_ID, ST_IsSimple(ST_GeometryFromText(everyplace_geometry)) \"Is Simple?\" FROM EVERYPLACE order by everyplace_ID");
        assertQuery("SELECT collection_ID, ST_IsSimple(ST_GeometryFromText(collection_multi)) \"Is Simple?\" FROM COLLECTION order by collection_ID");
        // ST_IsSimple02.rxp
        assertQuery("SELECT geoID,geoType,ST_IsSimple(ST_GeometryFromText(wkt)) \"Is it simple?\" FROM GEOMETRY_TEST order by geoID,geoType");
        assertQuery("SELECT geoID, geoType, ST_IsSimple(ST_GeometryFromText(wkt)) \"Should be 1\" FROM GEOMETRY_TEST WHERE ST_IsSimple(ST_GeometryFromText(wkt)) <> false order by geoID,geoType");
        // ST_IsSimple03.rxp
        assertQueryFails("SELECT ST_IsSimple(13) FROM GEOMETRY_TEST WHERE  geoID=1", "line 1:8: Unexpected parameters (integer) for function st_issimple. Expected: st_issimple(Geometry) ");
        assertQuery("SELECT ST_IsSimple(NULL) FROM GEOMETRY_TEST WHERE geoID=1");
        assertQueryFails("SELECT ST_IsSimple('error') FROM GEOMETRY_TEST WHERE geoID=1", "line 1:8: Unexpected parameters (varchar(5)) for function st_issimple. Expected: st_issimple(Geometry) ");
    }

    @Test
    public void testStIsClosed()
    {
        // st_isclosed.rxp
        assertQuery("select road_id,ST_isclosed(ST_GeometryFromText(road)) isclosed from ROADS order by road_id");
        assertQuery("select road_id, route,ST_isclosed(ST_GeometryFromText(road)) isclosed from ROADS where ST_isclosed(ST_GeometryFromText(road)) = false order by road_id");
        assertQuery("select county_id, ST_isclosed(ST_exteriorring(ST_GeometryFromText(county))) isclosed from COUNTIES order by county_id");
    }

    @Test
    public void testStIsRing()
    {
        // st_isring.rxp
        assertQuery("select ST_isring(ST_exteriorRing(ST_GeometryFromText(county))) IsRing from COUNTIES order by IsRing");
        assertQuery("select name, ST_isring(ST_exteriorRing(ST_GeometryFromText(county))) IsRing from COUNTIES where ST_isring(ST_exteriorRing(ST_GeometryFromText(county))) = false order by name");
        assertQuery("select route, ST_isRing(ST_GeometryFromText(road)) isRing from ROADS order by route");
        assertQuery("select route, ST_isRing(ST_GeometryFromText(road)) isRing from ROADS where ST_isRing(ST_GeometryFromText(road)) = false order by route");
        // st_isring8.rxp
        assertQuery("select id, ST_isring(ST_GeometryFromText(wkt)) IsRing from curves order by id");
        assertQuery("select id, ST_isring(ST_exteriorRing(ST_GeometryFromText(wkt))) IsRing from polygons order by id");
    }

    @Test
    public void testStIsEmpty()
    {
        // ST_IsEmpty01.rxp
        assertQuery("SELECT place_ID,ST_IsEmpty(ST_GeometryFromText(place_pt)) \"Is it empty?\" FROM PLACES order by place_ID");
        assertQuery("SELECT road_ID,ST_IsEmpty(ST_GeometryFromText(road))  \"Is it empty?\" FROM ROADS order by road_ID");
        assertQuery("SELECT county_ID, ST_IsEmpty(ST_GeometryFromText(county))  \"Is it empty?\" FROM COUNTIES order by  county_ID");
        assertQuery("SELECT ST_IsEmpty(ST_GeometryFromText(p1.place_pt)) \"Is p1 Empty?\", ST_IsEmpty(ST_GeometryFromText(p2.place_pt)) \"Is p2 Empty?\", ST_IsEmpty(ST_Union(ST_GeometryFromText(p1.place_pt),ST_GeometryFromText(p2.place_pt))) \"Is Union Empty?\" FROM PLACES as p1, PLACES as p2 order by 1, 2");
        assertQuery("SELECT ST_IsEmpty(ST_GeometryFromText(r1.road)) \"Is r1 Empty?\", ST_IsEmpty(ST_GeometryFromText(r2.road)) \"Is r2 Empty?\", ST_IsEmpty(ST_Union(ST_GeometryFromText(r1.road),ST_GeometryFromText(r2.road))) \"Is Union Empty?\" FROM ROADS as r1, ROADS as r2 order  by 1, 2");
        assertQuery("SELECT ST_IsEmpty(ST_GeometryFromText(c1.county)) \"Is c1 Empty?\", ST_IsEmpty(ST_GeometryFromText(c2.county)) \"Is c2 Empty?\", ST_IsEmpty(ST_Union(ST_GeometryFromText(c1.county),ST_GeometryFromText(c2.county))) \"Is Union Empty?\" FROM COUNTIES as c1, COUNTIES as c2 order  by 1, 2");
        assertQuery("SELECT drainage_ID, ST_IsEmpty(ST_GeometryFromText(stream_path)) \"Is Empty?\" FROM DRAINAGE order by drainage_ID");
        assertQuery("SELECT department_ID, ST_IsEmpty(ST_GeometryFromText(employee_loc)) \"Is Empty?\" FROM DEPARTMENT order by department_ID");
        assertQuery("SELECT saleszone_ID, ST_IsEmpty(ST_GeometryFromText(zone_area)) \"Is Empty?\" FROM SALESZONES order by saleszone_ID");
        assertQuery("SELECT everyplace_ID, ST_IsEmpty(ST_GeometryFromText(everyplace_geometry)) \"Is Empty?\" FROM EVERYPLACE order by everyplace_ID");
        assertQuery("SELECT collection_ID, ST_IsEmpty(ST_GeometryFromText(collection_multi)) \"Is Empty?\" FROM COLLECTION order by collection_ID");
        // ST_IsEmpty02.rxp
        assertQuery("SELECT geoID,geoType,ST_IsEmpty(ST_GeometryFromText(wkt)) \"Is it empty?\" FROM GEOMETRY_TEST order by geoID,geoType");
        assertQuery("SELECT geoID, geoType, ST_IsEmpty(ST_GeometryFromText(wkt)) \"Should be 1\" FROM GEOMETRY_TEST WHERE ST_IsEmpty(ST_GeometryFromText(wkt)) <> false order by geoID,geoType");
        // ST_IsEmpty03.rxp
        assertQueryFails("SELECT ST_IsEmpty(13) FROM GEOMETRY_TEST WHERE geoID=1", "line 1:8: Unexpected parameters (integer) for function st_isempty. Expected: st_isempty(Geometry) ");
        assertQuery("SELECT ST_IsEmpty(NULL) FROM GEOMETRY_TEST WHERE geoID=1");
        assertQueryFails("SELECT ST_IsEmpty('error') FROM GEOMETRY_TEST WHERE geoID=1", "line 1:8: Unexpected parameters (varchar(5)) for function st_isempty. Expected: st_isempty(Geometry) ");
        // ST_IsEmpty08.rxp
        assertQuery("SELECT id, ST_IsEmpty(ST_GeometryFromText(wkt)) \"Is it empty?\" FROM curves order by ID");
        assertQuery("SELECT id, ST_IsEmpty(ST_GeometryFromText(wkt)) \"Is it empty?\" FROM points order by ID");
        assertQuery("SELECT id, ST_IsEmpty(ST_GeometryFromText(wkt)) \"Is it empty?\" FROM surfaces order by ID");
        assertQuery("SELECT id, ST_IsEmpty(ST_GeometryFromText(wkt)) \"Is it empty?\" FROM polygons order by ID");
        assertQuery("SELECT id, ST_IsEmpty(ST_GeometryFromText(wkt)) \"Is it empty?\" FROM multilines order by ID");
    }

    @Test
    public void testStLength()
    {
        // st_length.rxp
        assertQuery("select ST_length(ST_GeometryFromText(road)) length from ROADS order by length");
        assertQuery("select route, ST_length(ST_GeometryFromText(road)) length from ROADS where ST_length(ST_GeometryFromText(road)) > 0 order by route, length");
    }

    @Test
    public void testStPointN()
    {
        // ST_PointN01.rxp
        assertQuery("SELECT road_Id, cast(ST_AsText(ST_PointN(ST_GeometryFromText(road),2)) as varchar(100)) \"The 2nd vertice\" FROM ROADS ORDER BY road_Id");
        // ST_PointN02.rxp
        assertQueryFails("SELECT place_ID,cast(ST_AsText(ST_PointN(ST_GeometryFromText(place_pt),2)) as varchar(100)) \"The 2nd vertice\" FROM PLACES ORDER BY place_ID", " ST_PointN only applies to LineString. Input type is: Point presto.default.st_pointn(presto.default.st_geometryfromtext(place_pt), 2:INTEGER) Top-level Expression: presto.default.substr(presto.default.st_astext(presto.default.st_pointn(presto.default.st_geometryfromtext(place_pt), 2:INTEGER)), 1:BIGINT, 100:BIGINT)");
        // ST_PointN08.rxp
        assertQuery("SELECT Id, cast(ST_AsText(ST_PointN(ST_GeometryFromText(wkt),1)) as varchar(100)) \"The 1st point\" FROM lines ORDER BY Id");
        assertQuery("SELECT Id, cast(ST_AsText(ST_PointN(ST_GeometryFromText(wkt),2)) as varchar(100)) \"The 2nd point\" FROM lines where ST_NumPoints(ST_GeometryFromText(wkt))>1 ORDER BY Id");
        assertQuery("SELECT Id, cast(ST_AsText(ST_PointN(ST_GeometryFromText(wkt),3)) as varchar(100)) \"The 3rd point\" FROM lines where ST_NumPoints(ST_GeometryFromText(wkt))>2 ORDER BY Id");
    }

    @Test
    public void testStNumPoints()
    {
        // ST_NumPoints01.rxp
        assertQuery("SELECT road_ID,ST_NumPoints(ST_GeometryFromText(road)) \"Number of Points\" FROM ROADS ORDER BY road_ID");
        assertQuery("SELECT road_ID,cast(ST_AsText(ST_GeometryFromText(road)) as varchar(400)) \"LineString\" FROM ROADS WHERE ST_NumPoints(ST_GeometryFromText(road)) > 2 ORDER BY road_ID");
        // ST_NumPoints03.rxp
        assertQuery("SELECT place_ID,ST_NumPoints(ST_GeometryFromText(place_pt)) \"Number of Points\" FROM PLACES ORDER BY place_id");
        assertQueryFails("SELECT road_ID,ST_NumPoints(13.23) \"Number of Points\" FROM ROADS WHERE road_ID = 1", "line 1:16: Unexpected parameters (decimal(4,2)) for function st_numpoints. Expected: st_numpoints(Geometry) ");
        assertQuery("SELECT road_ID, ST_NumPoints(NULL) \"Number of Points\" FROM ROADS WHERE road_ID = 100");
        assertQueryFails("SELECT road_ID,ST_NumPoints(ST_GeometryFromText('error')) \"Number of Points\" FROM ROADS WHERE road_ID = 100", "Invalid WKT: Expected word but found End-of-Stream (line 1)");
        // ST_NumPoints08.rxp
        assertQuery("SELECT ID,ST_NumPoints(ST_GeometryFromText(wkt)) \"Number of Points\" FROM lines ORDER BY ID");
    }

    @Test
    public void testStCentroid()
    {
        // st_centroid.rxp
        assertQuery("select ST_XMin(ST_centroid(ST_GeometryFromText(county))) centriod from COUNTIES order by centriod");
        assertQuery("select name, ST_XMin(ST_centroid(ST_GeometryFromText(county))) centroid from COUNTIES where ST_centroid(ST_GeometryFromText(county)) is not null order by name");
    }

    @Test
    public void testStDistance()
    {
        // st_distance.rxp
        assertQuery("select r1.road_id \"line1\", r2.road_id \"line2\",round(ST_distance(ST_GeometryFromText(r1.road),ST_GeometryFromText(r2.road)),12) \"distance(line,line)\" from roads r1, roads r2 order by r1.road_id,r2.road_id");
        assertQuery("select c1.county_id \"poly1\", c2.county_id \"poly2\", round(ST_distance(ST_GeometryFromText(c1.county), ST_GeometryFromText(c2.county)),12) \"distance(poly,poly)\" from counties c1, counties c2 order by c1.county_id, c2.county_id");
        assertQuery("select p1.place_id \"point1\", p2.place_id \"point2\", round(ST_distance(ST_GeometryFromText(p1.place_pt), ST_GeometryFromText(p2.place_pt)),12) \"distance(point,point)\" from places p1, places p2 order by p1.place_id,p2.place_id");
        assertQuery("select place_id, county_id, round(ST_distance(ST_GeometryFromText(place_pt), ST_GeometryFromText(county)),12) \"distance(point,poly)\" from places, counties order by place_id, county_id");
        assertQuery("select place_id, road_id, round(ST_distance(ST_GeometryFromText(place_pt), ST_GeometryFromText(road)),12) \"distance(point,line)\" from roads,places order by place_id, road_id");
        assertQuery("select county_id,road_id, round(ST_distance(ST_GeometryFromText(county), ST_GeometryFromText(road)),12) \"distance(poly,line)\" from roads, counties order by county_id, road_id");
        assertQuery("select county_id, road_id, round(ST_distance(ST_GeometryFromText(county), ST_GeometryFromText(road)),12) \"distance(poly,line)\" from roads, counties where round(ST_distance(ST_GeometryFromText(county), ST_GeometryFromText(road)),12) > 0 order by county_id, road_id");
        assertQuery("select county_id, road_id, round(ST_distance(ST_GeometryFromText(county), ST_GeometryFromText(road)),12) \"distance(poly,line)\" from roads, counties where round(ST_distance(ST_GeometryFromText(county), ST_GeometryFromText(road)),12) = 0 order by county_id, road_id");
        assertQuery("select place_id, road_id, round(ST_distance(ST_GeometryFromText(place_pt), ST_GeometryFromText(road)),12) \"distance(point,line)\" from roads, places where round(ST_distance(ST_GeometryFromText(place_pt), ST_GeometryFromText(road)),12) = 10 order by place_id, road_id");
        assertQuery("select saleszone_id, road_id, round(ST_distance(ST_GeometryFromText(zone_area), ST_GeometryFromText(road)),12) \"distance(multipoly,line)\" from saleszones, roads order by saleszone_id, road_id");
        assertQuery("select drainage_id, county_id, round(ST_distance(ST_GeometryFromText(stream_path), ST_GeometryFromText(county)),12) \"distance(multiline,poly)\" from drainage, counties order by drainage_id, county_id");
        assertQuery("select department_id, county_id, round(ST_distance(ST_GeometryFromText(employee_loc), ST_GeometryFromText(county)),12) \"distance(multipoint,poly)\" from department, counties order by department_id, county_id");
        // st_distance8.rxp
        assertQuery("select p1.id , p2.id , round(ST_distance(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)),8) \"distance\", round(ST_distance(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)),8) \"distance_Foot\", round(ST_distance(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)),8) \"distance_Meter\" from points p1, points p2 where p1.id= 90 and  p2.id= 91");
        assertQuery("select p1.id , p2.id , round(ST_distance(ST_GeometryFromText(p1.wkt) , ST_GeometryFromText(p2.wkt)),8) \"distance\", round(ST_distance(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)),8) \"distance_Foot\", round(ST_distance(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)),8) \"distance_Meter\" from points p1, points p2 where p1.id= 90 and  p2.id= 96");
        assertQuery("select p1.id , p2.id , round(ST_distance(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)),8) \"distance\", round(ST_distance(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)),8) \"distance_Foot\", round(ST_distance(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)),8) \"distance_Meter\" from points p1, points p2 where p1.id= 91 and  p2.id= 90");
        assertQuery("select p1.id , p2.id , round(ST_distance(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)),8) \"distance\", round(ST_distance(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)),8) \"distance_Foot\", round(ST_distance(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)),8) \"distance_Meter\" from points p1, points p2 where p1.id= 91 and  p2.id= 95");
        assertQuery("select p1.id , p2.id , round(ST_distance(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)),8) \"distance\", round(ST_distance(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)),8) \"distance_Foot\", round(ST_distance(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)),8) \"distance_Meter\" from points p1, points p2 where p1.id= 95 and  p2.id= 96");
        assertQuery("select p1.id , p2.id , round(ST_distance(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)),8) \"distance\", round(ST_distance(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)),8) \"distance_Foot\", round(ST_distance(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)),8) \"distance_Meter\" from points p1, points p2 where p1.id= 90 and  p2.id= 96");
        assertQuery("select p1.id , p2.id , round(ST_distance(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)),8) \"distance_Foot\" from points p1, points p2 where p1.id= 90 and  p2.id= 91");
        assertQuery("select p1.id , p2.id , round(ST_distance(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)),8) \"distance\", round(ST_distance(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)),8) \"distance_Foot\", round(ST_distance(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)),8) \"distance_Meter\" from points p1, points p2 where p1.id= 90 and  p2.id= 91");
        assertQuery("select p1.id , p2.id , round(ST_distance(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)),8) \"distance\", round(ST_distance(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)),8) \"distance_Foot\", round(ST_distance(ST_GeometryFromText(p1.wkt), ST_GeometryFromText(p2.wkt)),8) \"distance_Meter\" from points p1, points p2 where p1.id= 91 and  p2.id= 90");
    }

    @Test
    public void testStGeometryType()
    {
        // ST_GeomType01.rxp
        assertQuery("SELECT place_ID,ST_GeometryType(ST_GeometryFromText(place_pt)) \"Geometry Type\" FROM PLACES order by place_ID");
        assertQuery("SELECT road_ID,ST_GeometryType(ST_GeometryFromText(road)) \"Geometry Type\" FROM ROADS order by road_ID");
        assertQuery("SELECT county_ID,ST_GeometryType(ST_GeometryFromText(county)) \"Geometry Type\" FROM COUNTIES order by county_ID");
        assertQuery("SELECT p1.place_ID \"p1_ID\", ST_GeometryType(ST_GeometryFromText(p1.place_pt)) \"p1_GeometryType\", p2.place_ID \"p2_ID\", ST_GeometryType(ST_GeometryFromText(p2.place_pt)) \"p2_GeometryType\", ST_GeometryType(ST_Union(ST_GeometryFromText(p1.place_pt),ST_GeometryFromText(p2.place_pt))) \"Union_GeometryType\" FROM PLACES as p1,PLACES as p2 order by p1.place_ID,p2.place_ID");
        // mismatch between ST_Union for polygon empty and linestring empty presto vs c++ one return polygon/linestring other returns multipolygon/multilinestring
//        assertQuery("SELECT r1.road_ID \"r1_ID\", ST_GeometryType(ST_GeometryFromText(r1.road)) \"r1_GeometryType\", r2.road_ID \"r2_ID\", ST_GeometryType(ST_GeometryFromText(r2.road)) \"r2_GeometryType\", ST_GeometryType(ST_Union(ST_GeometryFromText(r1.road),ST_GeometryFromText(r2.road))) \"Union_GeometryType\" FROM ROADS as r1, ROADS as r2 order by r1.road_ID,r2.road_ID");
//        assertQuery("SELECT c1.county_ID \"c1_ID\", ST_GeometryType(ST_GeometryFromText(c1.county)) \"c1_GeometryType\", c2.county_ID \"c2_ID\", ST_GeometryType(ST_GeometryFromText(c2.county)) \"c2_GeometryType\", ST_GeometryType(ST_Union(ST_GeometryFromText(c1.county),ST_GeometryFromText(c2.county))) \"Union_GeometryType\" FROM COUNTIES as c1, COUNTIES as c2 order by c1.county_ID,c2.county_ID");
        assertQuery("SELECT drainage_ID, ST_GeometryType(ST_GeometryFromText(stream_path)) \"GeometryType\" FROM DRAINAGE order by drainage_ID");
        assertQuery("SELECT department_ID, ST_GeometryType(ST_GeometryFromText(employee_loc)) \"GeometryType\" FROM DEPARTMENT order by department_ID");
        assertQuery("SELECT saleszone_ID, ST_GeometryType(ST_GeometryFromText(zone_area)) \"GeometryType\" FROM SALESZONES order by saleszone_ID");
        assertQuery("SELECT everyplace_ID, ST_GeometryType(ST_GeometryFromText(everyplace_geometry)) \"GeometryType\" FROM EVERYPLACE order by everyplace_ID");
        assertQuery("SELECT collection_ID, ST_GeometryType(ST_GeometryFromText(collection_multi)) \"GeometryType\" FROM COLLECTION order by collection_ID");
        // ST_GeomType02.rxp
        assertQuery("SELECT geoID,geoType,ST_GeometryType(ST_GeometryFromText(wkt)) \"Geometry Type\" FROM GEOMETRY_TEST order by geoID,geoType");
        // ST_GeomType03.rxp
        assertQueryFails("SELECT geoID,geoType,ST_GeometryType(13) \"Geometry Type\" FROM GEOMETRY_TEST ORDER BY geoID,geoType", "line 1:22: Unexpected parameters (integer) for function st_geometrytype. Expected: st_geometrytype(Geometry) ");
        assertQuery("SELECT geoID,geoType,ST_GeometryType(NULL) \"Geometry Type\" FROM GEOMETRY_TEST ORDER BY geoID,geoType");
        assertQueryFails("SELECT geoID,geoType,ST_GeometryType(ST_GeometryFromText('error')) \"Geometry Type\" FROM GEOMETRY_TEST ORDER BY geoID,geoType", "Invalid WKT: Expected word but found End-of-Stream (line 1)");
        // ST_GeomType08.rxp
        assertQuery("SELECT ID,ST_GeometryType(ST_GeometryFromText(wkt)) \"Geometry Type\" FROM geometries order by ID");
        assertQuery("SELECT ID,ST_GeometryType(ST_GeometryFromText(wkt)) \"Geometry Type\" FROM collections order by ID");
        assertQuery("SELECT ID,ST_GeometryType(ST_GeometryFromText(wkt)) \"Geometry Type\" FROM curves order by ID");
        assertQuery("SELECT ID,ST_GeometryType(ST_GeometryFromText(wkt)) \"Geometry Type\" FROM surfaces order by ID");
        assertQuery("SELECT ID,ST_GeometryType(ST_GeometryFromText(wkt)) \"Geometry Type\" FROM polygons order by ID");
    }

    @Test
    public void testStStartPoint()
    {
        // st_startpoint.rxp
        assertQuery("select road_id, cast(ST_asText(ST_startpoint(ST_GeometryFromText(road))) as varchar(80)) start_point from ROADS order by road_id");
        // st_startpoint8.rxp
        assertQuery("select id, cast(ST_asText(ST_startpoint(ST_GeometryFromText(wkt))) as varchar(80)) start_point from curves order by id");
    }

    @Test
    public void testStEndPoint()
    {
        // st_endpoint.rxp
        assertQuery("select road_id, cast(ST_asText(ST_endpoint(ST_GeometryFromText(road))) as varchar(80)) end_point from ROADS order by road_id");
        // st_endpoint8.rxp
        assertQuery("select id, cast(ST_asText(ST_endpoint(ST_GeometryFromText(wkt))) as varchar(100)) end_point from curves order by id");
    }

    @Test
    public void testStGeometryN()
    {
        // ST_GeometryN01.rxp
        assertQuery("SELECT collection_ID, cast(ST_AsText(ST_GeometryN(ST_GeometryFromText(collection_multi),1)) as varchar(500)) \"The nth Geometry #1\" FROM COLLECTION ORDER BY collection_ID");
        // ST_GeometryN03.rxp
        assertQuery("SELECT collection_ID, cast(ST_AsText(ST_GeometryN(ST_GeometryFromText(collection_multi),-1)) as varchar(50)) \"The nth Geometry #1\" FROM COLLECTION WHERE collection_ID = 1502 ORDER BY collection_ID");
        assertQuery("SELECT road_ID, cast(ST_AsText(ST_GeometryN(ST_GeometryFromText(road),1)) as varchar(50)) \"The nth Geometry #1\" FROM ROADS ORDER BY road_ID");
        // ST_GeometryN08.rxp
        assertQuery("SELECT ID, cast(ST_AsText(ST_GeometryN(ST_GeometryFromText(wkt),1)) as varchar(500)) \"Geometry #1\" FROM collections ORDER BY ID");
        assertQuery("SELECT ID, cast(ST_AsText(ST_GeometryN(ST_GeometryFromText(wkt),2)) as varchar(500)) \"Geometry #2\" FROM collections where ST_NumGeometries(ST_GeometryFromText(wkt)) > 1 ORDER BY ID");
        assertQuery("SELECT ID, cast(ST_AsText(ST_GeometryN(ST_GeometryFromText(wkt),3)) as varchar(500)) \"Geometry #3\" FROM collections where ST_NumGeometries(ST_GeometryFromText(wkt)) > 2 ORDER BY ID");
    }

    @Test
    public void testStInteriorRingN()
    {
        // ST_IntRingN01.rxp
        assertQuery("SELECT county_ID, cast(ST_AsText(ST_InteriorRingN(ST_GeometryFromText(county),1)) as varchar(50)) \"The 1st Interior Ring\" FROM COUNTIES ORDER BY county_ID");
        // ST_IntRingN03.rxp
        assertQuery("SELECT county_ID, cast(ST_AsText(ST_InteriorRingN(ST_GeometryFromText(county),0)) as varchar(50)) \"The 0th Interior Ring\" FROM COUNTIES ORDER BY county_ID");
        // ST_IntRingN08.rxp
        assertQuery("SELECT ID, cast(ST_AsText(ST_InteriorRingN(ST_GeometryFromText(wkt),1)) as varchar(70)) \"The 1st Interior Ring\" FROM polygons where ST_numinteriorring(ST_GeometryFromText(wkt)) > 0 order by id");
        assertQuery("SELECT ID, cast(ST_AsText(ST_InteriorRingN(ST_GeometryFromText(wkt),2)) as varchar(64)) \"The 2nd Interior Ring\" FROM polygons where ST_numinteriorring(ST_GeometryFromText(wkt)) > 1 order by id");
    }

    @Test
    public void testStNumGeometries()
    {
        // ST_NumGeometries01.rxp
        assertQuery("SELECT collection_ID, ST_NumGeometries(ST_GeometryFromText(collection_multi)) \"Number of geometries\" FROM COLLECTION ORDER BY collection_ID");
        // ST_NumGeometries03.rxp
        assertQuery("SELECT place_ID, ST_NumGeometries(ST_GeometryFromText(place_pt)) \"Number of geometries\" FROM PLACES");
        assertQuery("SELECT collection_ID, ST_NumGeometries(NULL) \"Number of geometries\" FROM COLLECTION where collection_id=1500");
        // ST_NumGeometries08.rxp
        assertQuery("SELECT ID, ST_NumGeometries(ST_GeometryFromText(wkt)) \"Number of geometries\" FROM collections ORDER BY ID");
    }

    @Test
    public void testStNumInteriorRing()
    {
        // ST_NumIntRing01.rxp
        assertQuery("SELECT county_ID, ST_NumInteriorRing(ST_GeometryFromText(county))  \"Number of Interior Rings\" FROM COUNTIES ORDER BY county_ID");
        // ST_NumIntRing03.rxp
        assertQueryFails("SELECT place_ID, ST_NumInteriorRing(ST_GeometryFromText(place_pt))  \"Number of Interior Rings\" FROM PLACES", " ST_NumInteriorRing only applies to Polygon. Input type is: Point Top-level Expression: presto.default.st_numinteriorring(presto.default.st_geometryfromtext(place_pt))");
        assertQueryFails("SELECT county_ID, ST_NumInteriorRing(cast(12 as ST_Polygon)) \"Number of Interior Rings\" FROM COUNTIES WHERE  county_ID = 1", "line 1:38: Unknown type: st_polygon");
        // ST_NumIntRing08.rxp
        assertQuery("SELECT ID, ST_NumInteriorRing(ST_GeometryFromText(wkt))  \"Number of Interior Rings\" FROM polygons order by id");
    }

    @Test
    public void testStConvexHull()
    {
        // Polygon order mismatch for commented out tests not necessarily wrong.
        // ST_ConvexHull.rxp
        assertQuery("SELECT road_id,cast(ST_ASTEXT(ST_ConvexHull(ST_GeometryFromText(road))) as varchar(256)) \"The convexhull\" FROM ROADS WHERE road_id = 9 or road_id = 10 ORDER BY road_id");
//        assertQuery("SELECT county_id, cast(ST_ASTEXT(ST_ConvexHull(ST_GeometryFromText(county))) as varchar(256)) \"The convexhull\" FROM COUNTIES ORDER BY county_id");
        assertQuery("SELECT  drainage_id,cast(ST_ASTEXT(ST_ConvexHull(ST_GeometryFromText(stream_path))) as varchar(256)) \"The convexhull\" FROM DRAINAGE  WHERE drainage_id = 504 or drainage_id = 505 ORDER BY drainage_id");
//        assertQuery("SELECT saleszone_id,cast(ST_ASTEXT(ST_ConvexHull(ST_GeometryFromText(zone_area))) as varchar(256)) \"The convexhull\" FROM SALESZONES ORDER BY saleszone_id");
        assertQuery("SELECT hs.county_id,sa.county_id,sa.state_name FROM COUNTIES sa, COUNTIES hs WHERE cast(ST_ASTEXT(ST_ConvexHull(ST_GeometryFromText(hs.county))) as varchar(256))= cast(ST_ASTEXT(ST_ConvexHull(ST_GeometryFromText(sa.county))) as varchar(256)) ORDER BY hs.county_id,sa.county_id,sa.state_name");
//        assertQuery("SELECT collection_id,cast(ST_ASTEXT(ST_ConvexHull(ST_GeometryFromText(collection_multi))) as varchar(256)) \"The convexhull\" FROM COLLECTION ORDER BY collection_id");
//        assertQuery("SELECT everyplace_id,cast(ST_ASTEXT(ST_ConvexHull(ST_GeometryFromText(everyplace_geometry))) as varchar(256)) \"The convexhull\" FROM EVERYPLACE ORDER BY everyplace_id");
        assertQueryFails("SELECT cast(ST_ASTEXT(ST_ConvexHull('errortest')) as varchar(128)) \"The convexhull\" FROM EVERYPLACE", "line 1:23: Unexpected parameters (varchar(9)) for function st_convexhull. Expected: st_convexhull(Geometry) ");
        // ST_ConvexHull8.rxp
//        assertQuery("SELECT id, cast(ST_AsText(ST_convexHull(ST_GeometryFromText(wkt))) as varchar(1000)) \"Convex Hull Surfaces\" FROM surfaces order by id");
//        assertQuery("SELECT id, cast(ST_AsText(ST_convexHull(ST_GeometryFromText(wkt))) as varchar(1000)) \"Convex Hull Multilines\" FROM multilines order by id");
    }

    @Test
    public void testStCoordDim()
    {
        // ST_CoordDim01.rxp
        assertQuery("SELECT place_ID,ST_CoordDim(ST_GeometryFromText(place_pt)) \"Coordinate_dimension\" FROM PLACES order by place_ID");
        assertQuery("SELECT road_ID,ST_CoordDim(ST_GeometryFromText(road)) \"Coordinate_dimension\" FROM ROADS order by road_ID");
        assertQuery("SELECT county_ID,ST_CoordDim(ST_GeometryFromText(county)) \"Coordinate_dimension\" FROM COUNTIES order by county_ID");
        assertQuery("SELECT p1.place_ID as id1, p2.place_ID as id2, ST_CoordDim(ST_GeometryFromText(p1.place_pt)) \"p1_Coord\", ST_CoordDim(ST_GeometryFromText(p2.place_pt)) \"p2_Coord\", ST_CoordDim(ST_Union(ST_GeometryFromText(p1.place_pt),ST_GeometryFromText(p2.place_pt))) \"Coordinate_Dimension\" FROM PLACES as p1, PLACES as p2 order by 1,2");
        assertQuery("SELECT r1.road_ID as id1, r2.road_ID as id2, ST_CoordDim(ST_GeometryFromText(r1.road)) \"r1_Coord\", ST_CoordDim(ST_GeometryFromText(r2.road)) \"r2_Coord\", ST_CoordDim(ST_Union(ST_GeometryFromText(r1.road),ST_GeometryFromText(r2.road))) \"Coordinate_Dimension\" FROM ROADS as r1, ROADS as r2 order  by 1, 2");
        assertQuery("SELECT c1.county_ID as id1, c2.county_ID as id2, ST_CoordDim(ST_GeometryFromText(c1.county)) \"c1_Coord\", ST_CoordDim(ST_GeometryFromText(c2.county)) \"c2_Coord\", ST_CoordDim(ST_Union(ST_GeometryFromText(c1.county),ST_GeometryFromText(c2.county))) \"Coordinate_Dimension\" FROM COUNTIES as c1, COUNTIES as c2 order by 1, 2");
        assertQuery("SELECT drainage_ID, ST_CoordDim(ST_GeometryFromText(stream_path)) \"Dimension\" FROM DRAINAGE order by drainage_ID");
        assertQuery("SELECT department_ID, ST_CoordDim(ST_GeometryFromText(employee_loc)) \"Dimension\" FROM DEPARTMENT order by 1");
        assertQuery("SELECT saleszone_ID, ST_CoordDim(ST_GeometryFromText(zone_area)) \"Dimension\" FROM SALESZONES order by saleszone_ID");
        assertQuery("SELECT everyplace_ID, ST_CoordDim(ST_GeometryFromText(everyplace_geometry)) \"Dimension\" FROM EVERYPLACE order by everyplace_ID");
        assertQuery("SELECT collection_ID, ST_CoordDim(ST_GeometryFromText(collection_multi)) \"Dimension\" FROM COLLECTION order by 1");
        // ST_CoordDim02.rxp
        assertQuery("SELECT geoID,geotype,ST_CoordDim(ST_GeometryFromText(wkt)) \"Coordinate_dimension\" FROM GEOMETRY_TEST order by geoID,geotype");
        assertQuery("SELECT geoID,geotype, ST_CoordDim(ST_GeometryFromText(wkt)) FROM GEOMETRY_TEST WHERE ST_CoordDim(ST_GeometryFromText(wkt)) > 1 order by geoID,geotype");
        // ST_CoordDim03.rxp
        assertQueryFails("SELECT geoID,geotype,ST_CoordDim(13) \"Coordinate_dimension\" FROM GEOMETRY_TEST ORDER BY geoID,geotype", "line 1:22: Unexpected parameters (integer) for function st_coorddim. Expected: st_coorddim(Geometry) ");
        assertQuery("SELECT geoID,geotype,ST_CoordDim(NULL) \"Coordinate_dimension\" FROM GEOMETRY_TEST ORDER BY geoID,geotype");
        assertQueryFails("SELECT geoID,geotype,ST_CoordDim('error') \"Coordinate_dimension\" FROM GEOMETRY_TEST ORDER BY geoID,geotype", "line 1:22: Unexpected parameters (varchar(5)) for function st_coorddim. Expected: st_coorddim(Geometry) ");
        // ST_CoordDim08.rxp
        assertQuery("SELECT ID,ST_CoordDim(ST_GeometryFromText(wkt)) \"Coordinate_dimension\" FROM points order by ID");
        assertQuery("SELECT ID,ST_CoordDim(ST_GeometryFromText(wkt)) \"Coordinate_dimension\" FROM curves order by ID");
        assertQuery("SELECT ID,ST_CoordDim(ST_GeometryFromText(wkt)) \"Coordinate_dimension\" FROM lines order by ID");
        assertQuery("SELECT ID,ST_CoordDim(ST_GeometryFromText(wkt)) \"Coordinate_dimension\" FROM multilines order by ID");
        assertQuery("SELECT ID,ST_CoordDim(ST_GeometryFromText(wkt)) \"Coordinate_dimension\" FROM multipoints order by ID");
    }

    @Test
    public void testStDimension()
    {
        assertQuery("SELECT place_ID,ST_Dimension(ST_GeometryFromText(place_pt)) \"Dimension\" FROM PLACES order by place_id");
        assertQuery("SELECT road_ID,ST_Dimension(ST_GeometryFromText(road)) \"Dimension\" FROM ROADS order by road_ID");
        assertQuery("SELECT county_ID,ST_Dimension(ST_GeometryFromText(county)) \"Dimension\" FROM COUNTIES order by county_ID");
        assertQuery("SELECT ST_GeometryType(ST_Union(ST_GeometryFromText(p1.place_pt),ST_GeometryFromText(p2.place_pt))) \"GeometryType\", ST_Dimension(ST_Union(ST_GeometryFromText(p1.place_pt),ST_GeometryFromText(p2.place_pt))) \"Dimension\" FROM PLACES as p1, PLACES as p2 order by 1, 2");
        assertQuery("SELECT ST_GeometryType(ST_Union(ST_GeometryFromText(r1.road),ST_GeometryFromText(r2.road))) \"GeometryType\", ST_Dimension(ST_Union(ST_GeometryFromText(r1.road),ST_GeometryFromText(r2.road))) \"Dimension\" FROM ROADS as r1, ROADS as r2 order by 1, 2");
        // Mismatch of type getting cast within internal geometries. Example : [ST_Polygon, 2] vs [ST_MultiPolygon, 2]
//        assertQuery("SELECT ST_GeometryType(ST_Union(ST_GeometryFromText(c1.county),ST_GeometryFromText(c2.county))) \"GeometryType\", ST_Dimension(ST_Union(ST_GeometryFromText(c1.county),ST_GeometryFromText(c2.county))) \"Dimension\" FROM COUNTIES as c1, COUNTIES as c2 order  by 1, 2");
        assertQuery("SELECT drainage_ID, ST_GeometryType(ST_GeometryFromText(stream_path)) \"GeometryType\", ST_Dimension(ST_GeometryFromText(stream_path)) \"Dimension\" FROM DRAINAGE order by 1,2");
        assertQuery("SELECT department_ID, ST_GeometryType(ST_GeometryFromText(employee_loc)) \"GeometryType\", ST_Dimension(ST_GeometryFromText(employee_loc)) \"Dimension\" FROM DEPARTMENT order by 1, 2");
        assertQuery("SELECT saleszone_ID, ST_GeometryType(ST_GeometryFromText(zone_area)) \"GeometryType\", ST_Dimension(ST_GeometryFromText(zone_area)) \"Dimension\" FROM SALESZONES order by saleszone_ID");
        assertQuery("SELECT everyplace_ID, ST_GeometryType(ST_GeometryFromText(everyplace_geometry)) \"GeometryType\", ST_Dimension(ST_GeometryFromText(everyplace_geometry)) \"Dimension\" FROM EVERYPLACE order by 1, 2");
        assertQuery("SELECT collection_ID, ST_GeometryType(ST_GeometryFromText(collection_multi)) \"GeometryType\", ST_Dimension(ST_GeometryFromText(collection_multi)) \"Dimension\" FROM COLLECTION order by 1, 2");
        // ST_Dimension02.rxp
        assertQuery("SELECT geoID,geotype,ST_Dimension(ST_GeometryFromText(wkt)) \"Dimension\" FROM GEOMETRY_TEST order by geoID,geotype");
        assertQuery("SELECT geoID,geotype,ST_Dimension(ST_GeometryFromText(wkt)) \"Dimension\" FROM GEOMETRY_TEST WHERE ST_Dimension(ST_GeometryFromText(wkt)) > 1 order by geoID,geotype");
        // ST_Dimension03.rxp
        assertQueryFails("SELECT geoID,geotype, ST_Dimension(13) FROM GEOMETRY_TEST ORDER BY geoID,geotype", "line 1:23: Unexpected parameters (integer) for function st_dimension. Expected: st_dimension(Geometry) ");
        assertQuery("SELECT geoID,geotype, ST_Dimension(NULL) FROM GEOMETRY_TEST ORDER BY geoID,geotype");
        assertQueryFails("SELECT geoID,geotype, ST_Dimension('error') FROM GEOMETRY_TEST ORDER BY geoID,geotype", "line 1:23: Unexpected parameters (varchar(5)) for function st_dimension. Expected: st_dimension(Geometry) ");
        // ST_Dimension08.rxp
        assertQuery("SELECT id,ST_Dimension(ST_GeometryFromText(wkt)) \"Dimension\" FROM points order by id");
        assertQuery("SELECT id,ST_Dimension(ST_GeometryFromText(wkt)) \"Dimension\" FROM collections order by id");
        assertQuery("SELECT id,ST_Dimension(ST_GeometryFromText(wkt)) \"Dimension\" FROM multisurfaces order by id");
        assertQuery("SELECT id,ST_Dimension(ST_GeometryFromText(wkt)) \"Dimension\" FROM lines order by id");
        assertQuery("SELECT id,ST_Dimension(ST_GeometryFromText(wkt)) \"Dimension\" FROM polygons order by id");
    }

    @Test
    public void testStBuffer()
    {
        // ST_Buffer.rxp
        assertQuery("SELECT ST_Equals(ST_Buffer(ST_GeometryFromText(hs.place_pt),30),ST_Buffer(ST_GeometryFromText(hs.place_pt),30)) \"first thousand srs=3\" FROM PLACES hs WHERE hs.place_id = 200");
        assertQuery("SELECT substr(cast(ST_AsText(ST_Buffer(ST_GeometryFromText(hs.place_pt),30)) as varchar(5000)), 2001, 50) \"third thousand bytes capped\" FROM PLACES hs WHERE hs.place_id = 200");
        assertQuery("SELECT hs.place_id,cast(ST_AsText(ST_Buffer(ST_GeometryFromText(hs.place_pt),30)) as varchar(1000)) \"location\" FROM PLACES hs WHERE hs.place_id = 300 or hs.place_id = 400 ORDER BY hs.place_id");
        assertQuery("SELECT place_id, cast(ST_AsText(ST_Buffer(ST_GeometryFromText(place_pt),0)) as varchar(64)) \"location srs=3\" FROM PLACES order by place_id");
        assertQuery("SELECT road_id,ST_Equals(ST_Buffer(ST_GeometryFromText(road),5),ST_Buffer(ST_GeometryFromText(road),5)) \"first thousand bytes\" FROM ROADS WHERE road_id = 7 or road_id = 200 order by road_id");
        assertQuery("SELECT road_id,substr( cast(ST_AsText(ST_Buffer(ST_GeometryFromText(road),5)) as varchar(5000)),1001, 1000) \"second thousand bytes\" FROM ROADS WHERE road_id = 7 or road_id = 200 order by road_id");
        assertQuery("SELECT road_id,substr( cast(ST_AsText(ST_Buffer(ST_GeometryFromText(road),5)) as varchar(5000)),2001, 50) \"third thousand bytes capped\" FROM ROADS WHERE road_id = 7 or road_id = 200 order by road_id");
        assertQuery("SELECT road_id,substr( cast(ST_AsText(ST_Buffer(ST_GeometryFromText(road),2.4789)) as varchar(5000)),1, 1000) \"first thousand bytes\" FROM ROADS WHERE road_id = 8");
        assertQuery("SELECT road_id,substr( cast(ST_AsText(ST_Buffer(ST_GeometryFromText(road),2.4789)) as varchar(5000)), 1001, 1000) \"second thousand bytes\" FROM ROADS WHERE road_id = 8");
        assertQuery("SELECT road_id,substr( cast(ST_AsText(ST_Buffer(ST_GeometryFromText(road),2.4789)) as varchar(5000)) , 2001 , 50) \"third thousand bytes capped\" FROM ROADS WHERE road_id = 8");
        assertQuery("SELECT road_id,cast(ST_AsText(ST_Buffer(ST_GeometryFromText(road),2.4789)) as varchar(700)) \"location\" FROM ROADS WHERE road_id = 300 or road_id = 400 order by road_id");
        assertQuery("SELECT road_id,cast(ST_AsText(ST_Buffer(ST_GeometryFromText(road),0)) as varchar(700)) \"location\" FROM ROADS order by road_id");
        assertQuery("SELECT hs.county_id,substr( cast(ST_AsText(ST_Buffer(ST_GeometryFromText(hs.county),1.22)) as varchar(5000)),1, 1000) \"first thousand bytes\" FROM COUNTIES hs WHERE county_id <= 5 order by county_id");
        assertQuery("SELECT hs.county_id, substr( cast(ST_AsText(ST_Buffer(ST_GeometryFromText(hs.county),1.22)) as varchar(5000)), 1001, 50) \"second thousand bytes capped\" FROM COUNTIES hs WHERE county_id <= 5 order by county_id");
        assertQuery("SELECT hs.county_id,cast(ST_AsText(ST_Buffer(ST_GeometryFromText(hs.county),1.22)) as varchar(700)) \"location\" FROM COUNTIES hs WHERE county_id = 300 or county_id = 400 order by county_id");
        assertQuery("SELECT hs.county_id,cast(ST_AsText(ST_Buffer(ST_GeometryFromText(hs.county),0)) as varchar(700)) \"location\" FROM COUNTIES hs order by county_id");
        assertQueryFails("SELECT hs.county_id,cast(ST_AsText(ST_Buffer(ST_GeometryFromText(hs.county),-0.887645374378)) as varchar(512)) \"location\" FROM COUNTIES hs WHERE county_id <= 5 order by county_id", " Provided distance must not be negative. Provided distance: -0.887645374378 presto.default.st_buffer(presto.default.st_geometryfromtext(county), -0.887645374378:DOUBLE) Top-level Expression: presto.default.substr(presto.default.st_astext(presto.default.st_buffer(presto.default.st_geometryfromtext(county), -0.887645374378:DOUBLE)), 1:BIGINT, 512:BIGINT)");
        assertQuery("SELECT hs.place_id, sa.place_id, substr( cast(ST_AsText(ST_Buffer(ST_Union(ST_GeometryFromText(sa.place_pt), ST_GeometryFromText(hs.place_pt)),1.2)) as varchar(8000)),1, 1000) \"first thousand bytes\" FROM PLACES hs,PLACES sa WHERE sa.place_id  = 5 AND hs.place_id = 5");
        assertQuery("SELECT sa.road_id,hs.road_id, substr( cast(ST_AsText(ST_Buffer(ST_Union(ST_GeometryFromText(sa.road),ST_GeometryFromText(hs.road)), 14.4234)) as varchar(2048)),1, 1000) \"first thousand bytes\" FROM ROADS hs, ROADS sa WHERE sa.road_id = 4 AND hs.road_id = 5");
        assertQuery("SELECT sa.road_id,hs.road_id, ST_Equals(ST_Buffer(ST_Union(ST_GeometryFromText(sa.road),ST_GeometryFromText(hs.road)), 14.4234), ST_Buffer(ST_Union(ST_GeometryFromText(sa.road),ST_GeometryFromText(hs.road)), 14.4234)) \"second thousand bytes\" FROM ROADS hs,ROADS sa WHERE sa.road_id = 4 AND hs.road_id = 5");
        assertQuery("SELECT sa.county_id,hs.county_id, ST_Equals(ST_Buffer(ST_Union(ST_GeometryFromText(sa.county),ST_GeometryFromText(hs.county)),2), ST_Buffer(ST_Union(ST_GeometryFromText(sa.county),ST_GeometryFromText(hs.county)),2))  \"location\" FROM COUNTIES hs,COUNTIES sa WHERE sa.county_id = 4 AND hs.county_id = 5");
        assertQuery("SELECT sa.county_id,hs.county_id, ST_Equals(ST_Buffer(ST_Union(ST_GeometryFromText(sa.county),ST_GeometryFromText(hs.county)),2), ST_Buffer(ST_Union(ST_GeometryFromText(sa.county),ST_GeometryFromText(hs.county)),2))  \"location\" FROM COUNTIES hs,COUNTIES sa WHERE sa.county_id = 4 AND hs.county_id = 5");
        assertQuery("SELECT sa.county_id,hs.county_id, substr( cast(ST_AsText(ST_Buffer(ST_Union(ST_GeometryFromText(sa.county),ST_GeometryFromText(hs.county)),2)) as varchar(5000)), 2001, 50) \"location capped\" FROM COUNTIES hs, COUNTIES sa WHERE sa.county_id = 4 AND hs.county_id = 5");
        assertQuery("SELECT cast(ST_AsText(ST_Buffer(ST_GeometryFromText(hs.place_pt),30)) as varchar(100)) \"location\" FROM PLACES hs ORDER BY hs.place_id");
        assertQueryFails("SELECT cast(ST_AsText(ST_Buffer('errortest',3)) as varchar(512)) \"location\" FROM SALESZONES", "line 1:23: Unexpected parameters (varchar(9), integer) for function st_buffer. Expected: st_buffer(Geometry, double) ");
        assertQuery("SELECT county_id, cast(ST_AsText(ST_GeometryFromText(county)) as varchar(700)) \"location srs = 0\" FROM COUNTIES WHERE county_id = 4");
        assertQuery("SELECT county_id, cast(ST_AsText(ST_Buffer(ST_GeometryFromText(county), 1.22)) as varchar(900)) \"location srs = 0\" FROM COUNTIES WHERE county_id =4");
        assertQuery("SELECT county_id, cast(ST_AsText(ST_GeometryFromText(county)) as varchar(700)) \"location srs = 3\" FROM COUNTIES WHERE county_id = 4");
        assertQuery("SELECT county_id, substr(cast(ST_AsText(ST_Buffer(ST_GeometryFromText(county), 1.22)) as varchar(2000)),1, 1000) \"location srs=3 1rst thousand\" FROM COUNTIES WHERE county_id =4");
        assertQuery("SELECT county_id, substr(cast(ST_AsText(ST_Buffer(ST_GeometryFromText(county), 1.22)) as varchar(2000)),1001, 50) \"location srs=3 2nd thousand capped\" FROM COUNTIES WHERE county_id =4");
        // ST_Buffer8.rxp
        assertQuery("SELECT id, ST_Equals(ST_Buffer(ST_GeometryFromText(wkt),30), ST_Buffer(ST_GeometryFromText(wkt),30)) Buffer FROM polygons where id = 1003 or id=1013 order by id");
    }
}
