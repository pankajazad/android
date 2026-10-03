package org.cryptomator.domain.usecases.cloud;

import org.cryptomator.domain.CloudFolder;
import org.cryptomator.domain.CloudNode;
import org.cryptomator.domain.exception.BackendException;
import org.cryptomator.domain.repository.CloudContentRepository;
import org.cryptomator.generator.Parameter;
import org.cryptomator.generator.UseCase;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@UseCase
class GetCloudNodeListRecursive {

	private final CloudContentRepository cloudContentRepository;
	private final CloudFolder rootFolder;

	GetCloudNodeListRecursive(CloudContentRepository cloudContentRepository, @Parameter CloudFolder rootFolder) {
		this.cloudContentRepository = cloudContentRepository;
		this.rootFolder = rootFolder;
	}

	public List<CloudNode> execute() throws BackendException {
		List<CloudNode> nodes = new ArrayList<>();
		Deque<CloudFolder> foldersToVisit = new ArrayDeque<>();
		foldersToVisit.push(rootFolder);

		while (!foldersToVisit.isEmpty()) {
			List<CloudFolder> childFolders = new ArrayList<>();
			List<CloudNode> children = cloudContentRepository.list(foldersToVisit.pop());
			for (CloudNode child : children) {
				nodes.add(child);
				if (child instanceof CloudFolder) {
					childFolders.add((CloudFolder) child);
				}
			}
			for (int i = childFolders.size() - 1; i >= 0; i--) {
				foldersToVisit.push(childFolders.get(i));
			}
		}
		return nodes;
	}
}
