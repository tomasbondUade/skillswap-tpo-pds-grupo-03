
package ar.edu.uade.skillswap.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "publication_tags")
public class PublicationTagEntity {

    @EmbeddedId
    private PublicationTagId id;

    @MapsId("publicationId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publication_id", nullable = false)
    private PublicationEntity publication;

    public PublicationTagEntity() {
    }

    public PublicationTagId getId() {
        return id;
    }

    public PublicationEntity getPublication() {
        return publication;
    }

    public void setId(PublicationTagId id) {
        this.id = id;
    }

    public void setPublication(PublicationEntity publication) {
        this.publication = publication;
    }

    @Embeddable
    public static class PublicationTagId implements Serializable {

        @Column(name = "publication_id", length = 100, nullable = false)
        private String publicationId;

        @Column(name = "tag", length = 150, nullable = false)
        private String tag;

        protected PublicationTagId() {
        }

        public PublicationTagId(String publicationId, String tag) {
            this.publicationId = publicationId;
            this.tag = tag;
        }

        public String getPublicationId() {
            return publicationId;
        }

        public String getTag() {
            return tag;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof PublicationTagId that)) {
                return false;
            }
            return Objects.equals(publicationId, that.publicationId)
                    && Objects.equals(tag, that.tag);
        }

        @Override
        public int hashCode() {
            return Objects.hash(publicationId, tag);
        }
    }
}
