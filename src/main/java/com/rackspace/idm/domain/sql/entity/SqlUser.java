package com.rackspace.idm.domain.sql.entity;

import com.rackspace.idm.validation.MessageTexts;
import com.rackspace.idm.validation.RegexPatterns;
import lombok.Data;
import org.dozer.Mapping;
import org.hibernate.validator.constraints.Length;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.List;

@Data
@Entity
@Table(name = "user")
@NamedEntityGraph(name = "SqlUser.rax", attributeNodes = { @NamedAttributeNode("rax"), @NamedAttributeNode("rsGroupId") })
public class SqlUser {

    @Id
    @Column(name = "id")
    private String id;

    @NotNull
    @Length(min = 1, max = 100)
    @Pattern(regexp = RegexPatterns.USERNAME, message = MessageTexts.USERNAME)
    @Column(name = "name")
    private String username;

    @Column(name = "extra")
    private String extra;

    @Column(name = "password")
    private String userPassword;

    @Mapping("enabled")
    @Column(name = "enabled")
    protected Boolean enabled;

    @Column(name = "domain_id")
    private String domainId;

    @Column(name = "default_project_id")
    private Integer mossoId;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "id", referencedColumnName = "id", nullable = true)
    private SqlUserRax rax;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name="user_group_membership",
            joinColumns=@JoinColumn(name="user_id"))
    @Column(name="group_id")
    private List<String> rsGroupId;

}