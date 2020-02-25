package gov.usgs.wma.waterdata.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.github.springtestdbunit.annotation.DatabaseSetup;

import gov.usgs.wma.waterdata.springinit.BaseIT;

@SpringBootTest(webEnvironment=WebEnvironment.RANDOM_PORT)
@DatabaseSetup("classpath:/testData/monitoringLocation/")
public class CollectionsControllerIT extends BaseIT {
	@Autowired
	private TestRestTemplate restTemplate;

	public static final List<String> COLLECTION_IDS = List.of("AHS", "monitoring-locations");

	@Test
	public void collectionsToCollectionTest() {
		try {
			String collectionsJsonStr = doCollectionRequest("/collections");
			JSONObject collectionsJson = new JSONObject(collectionsJsonStr);
			assertTrue(collectionsJson.get("collections") instanceof JSONArray);
			JSONArray collections = (JSONArray) collectionsJson.get("collections");
			assertTrue(collections.length() == COLLECTION_IDS.size());
			ArrayList<String> returnedCollectionIds = new ArrayList<>();
			for(int i=0; i < collections.length(); i++) {
				assertTrue(collections.get(i) instanceof JSONObject);
				JSONObject collectionJson = (JSONObject) collections.get(i);
				String collectionId = collectionJson.getString("id");
				assertNotNull(collectionId);
				assertTrue(COLLECTION_IDS.contains(collectionId), "Unknown collection id returned: " + collectionId);
				assertFalse(returnedCollectionIds.contains(collectionId), "Duplicate collection id returned: " + collectionId);
				String returnedCollection = doCollectionRequest("/collections/" + collectionId);
				assertJsonEquals(collectionJson.toString(), returnedCollection);
				returnedCollectionIds.add(collectionId);
			}
		} catch (JSONException e) {
			fail("Unexpected JSONException during test", e);
		}
	}

	@Test
	public void getCollectionsTest() {
		try {
			doGetCollectionTest("/collections", "collections.json");
		} catch (IOException e) {
			fail("Unexpected IOException during test", e);
		}
	}

	@Test
	public void getMonLocsCollectionTest() {
		try {
			doGetCollectionTest("/collections/monitoring-locations", "monLocsCollection.json");
		} catch (IOException e) {
			fail("Unexpected IOException during test", e);
		}
	}

	@Test
	public void getNetworkCollectionTest() {
		try {
			doGetCollectionTest("/collections/AHS", "ahsCollection.json");
		} catch (IOException e) {
			fail("Unexpected IOException during test", e);
		}
	}

	@Test
	public void collectionNotFoundTest() {
		ResponseEntity<String> rtn = restTemplate.getForEntity("/collections/xyz", String.class);
		assertEquals(HttpStatus.NOT_FOUND.value(), rtn.getStatusCode().value());
		assertNull(rtn.getBody());
	}

	@Test
	public void getCollectionFeatureTest() {
		try {
			doGetCollectionTest("/collections/monitoring-locations/items/USGS-07227448",
					"features/monitoring-locations/USGS-07227448.json");
		} catch (IOException e) {
			fail("Unexpected IOException during test", e);
		}
	}

	@Test
	public void getNetworkCollectionFeatureTest() {
		try {
			doGetCollectionTest("/collections/AHS/items/USGS-343204093005501",
					"features/AHS/USGS-343204093005501.json");
		} catch (IOException e) {
			fail("Unexpected IOException during test", e);
		}
		// this feature should also be in the monitoring-locations collection
		try {
			doGetCollectionTest("/collections/monitoring-locations/items/USGS-343204093005501",
					"features/monitoring-locations/USGS-343204093005501.json");
		} catch (IOException e) {
			fail("Unexpected IOException during test", e);
		}
	}

	@Test
	public void collectionNotFoundItemsTest() {
		ResponseEntity<String> rtn = restTemplate.getForEntity("/collections/xyz/items/USGS-07227448", String.class);
		assertEquals(HttpStatus.NOT_FOUND.value(), rtn.getStatusCode().value());
		assertNull(rtn.getBody());
	}

	@Test
	public void featureNotFoundTest() {
		ResponseEntity<String> rtn = restTemplate.getForEntity("/collections/AHS/items/xyz", String.class);
		assertEquals(HttpStatus.NOT_FOUND.value(), rtn.getStatusCode().value());
		assertNull(rtn.getBody());
	}

	@Test
	public void featureNotFoundNoGeomTest() {
		ResponseEntity<String> rtn = restTemplate.getForEntity("/collections/monitoring-locations/items/USGS-0402809", String.class);
		assertEquals(HttpStatus.NOT_FOUND.value(), rtn.getStatusCode().value());
		assertNull(rtn.getBody());
	}

	@Test
	public void featureExistsInAnotherCollectionTest() {
		ResponseEntity<String> rtn = restTemplate.getForEntity("/collections/AHS/items/USGS-07227448", String.class);
		assertEquals(HttpStatus.NOT_FOUND.value(), rtn.getStatusCode().value());
		assertNull(rtn.getBody());
	}

	private void doGetCollectionTest(String path, String resultFile) throws IOException {
		String actual = doCollectionRequest(path);
		String expected = getCompareFile(resultFile);
		assertJsonEquals(expected, actual);
	}

	private String doCollectionRequest(String path) {
		ResponseEntity<String> rtn = restTemplate.getForEntity(path, String.class);
		assertEquals(rtn.getStatusCode().value(), HttpStatus.OK.value());
		assertNotNull(rtn);

		return rtn.getBody();
	}

}