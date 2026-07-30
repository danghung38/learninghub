package com.dxh.learninghub.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "roles")
@Entity
public class Role extends AbstractEntity<Long>{
    @Column(nullable = false, unique = true)
    String name;

    @Column(name = "description")
    String description;

    @ManyToMany
    @Builder.Default
    Set<Permission> permissions = new LinkedHashSet<>();
}
