package com.stuff;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "AN_ENTITY",
        indexes = {
                @Index(name = "AN_ENTITY_DATETIME", columnList = "DATE_TIME")
        })
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@Getter
public class AnEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @NonNull
    @Column(name = "DATE_TIME", nullable = false)
    private OffsetDateTime datetime;

    @NonNull
    @Column(name = "USERNAME", nullable = false)
    private String username;

    @NonNull
    @Column(name = "FILENAME", nullable = false)
    private String filename;

    @NonNull
    @Column(name = "DETAILS", nullable = false)
    private String details;
}
