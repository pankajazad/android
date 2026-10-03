package org.cryptomator.domain.usecases.cloud;

import org.cryptomator.domain.CloudFile;
import org.cryptomator.domain.CloudFolder;
import org.cryptomator.domain.CloudNode;
import org.cryptomator.domain.exception.BackendException;
import org.cryptomator.domain.repository.CloudContentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class GetCloudNodeListRecursiveTest {

	private CloudContentRepository cloudContentRepository;
	private CloudFolder rootFolder;
	private CloudFolder firstFolder;
	private CloudFolder nestedFolder;
	private CloudFile rootFile;
	private CloudFile nestedFile;
	private CloudFile deeplyNestedFile;

	@BeforeEach
	public void setUp() {
		cloudContentRepository = mock(CloudContentRepository.class);
		rootFolder = mock(CloudFolder.class);
		firstFolder = mock(CloudFolder.class);
		nestedFolder = mock(CloudFolder.class);
		rootFile = mock(CloudFile.class);
		nestedFile = mock(CloudFile.class);
		deeplyNestedFile = mock(CloudFile.class);
	}

	@Test
	public void listsNodesFromAllDescendantFolders() throws BackendException {
		when(cloudContentRepository.list(rootFolder)).thenReturn(asList(rootFile, firstFolder));
		when(cloudContentRepository.list(firstFolder)).thenReturn(asList(nestedFile, nestedFolder));
		when(cloudContentRepository.list(nestedFolder)).thenReturn(singletonList(deeplyNestedFile));

		List<CloudNode> result = testCandidate().execute();

		assertThat(result, is(asList(rootFile, firstFolder, nestedFile, nestedFolder, deeplyNestedFile)));
		verify(cloudContentRepository).list(rootFolder);
		verify(cloudContentRepository).list(firstFolder);
		verify(cloudContentRepository).list(nestedFolder);
		verifyNoMoreInteractions(cloudContentRepository);
	}

	@Test
	public void returnsNoNodesForAnEmptyFolder() throws BackendException {
		when(cloudContentRepository.list(rootFolder)).thenReturn(emptyList());

		List<CloudNode> result = testCandidate().execute();

		assertThat(result, is(emptyList()));
		verify(cloudContentRepository).list(rootFolder);
		verifyNoMoreInteractions(cloudContentRepository);
	}

	private GetCloudNodeListRecursive testCandidate() {
		return new GetCloudNodeListRecursive(cloudContentRepository, rootFolder);
	}
}
