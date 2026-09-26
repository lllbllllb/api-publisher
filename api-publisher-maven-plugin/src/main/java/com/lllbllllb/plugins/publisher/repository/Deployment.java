package com.lllbllllb.plugins.publisher.repository;

import org.eclipse.aether.repository.RemoteRepository;

import java.util.List;

/**
 * The publications that go to one repository, in one Resolver deploy request.
 *
 * @param repository   the target, with authentication and proxy applied
 * @param publications what to deploy there, in scan order
 */
public record Deployment(RemoteRepository repository, List<Publication> publications) {

    public Deployment {
        publications = List.copyOf(publications);
    }
}
