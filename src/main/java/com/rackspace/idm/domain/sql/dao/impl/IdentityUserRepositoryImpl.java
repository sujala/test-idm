package com.rackspace.idm.domain.sql.dao.impl;

import com.rackspace.idm.annotation.SQLRepository;
import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.domain.entity.FederatedUser;
import com.rackspace.idm.domain.entity.PaginatorContext;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.sql.dao.FederatedUserRepository;
import com.rackspace.idm.domain.sql.dao.IdentityUserRepository;
import com.rackspace.idm.domain.sql.dao.UserRepository;
import com.rackspace.idm.domain.sql.mapper.impl.FederatedUserRaxMapper;
import com.rackspace.idm.domain.sql.mapper.impl.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManager;
import java.math.BigInteger;
import java.util.*;

@SQLRepository
public class IdentityUserRepositoryImpl implements IdentityUserRepository {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FederatedUserRaxMapper federatedUserRaxMapper;

    @Autowired
    private FederatedUserRepository federatedUserRepository;

    private static final String COUNT_BY_DOMAIN_ID =
            "select count(*) from ( " +
                    "select id from user where " +
                    "domain_id = ? " +
                    "union " +
                    "select id from federated_user_rax where " +
                    "domain_id = ?) as u";

    private static final String ALL_BY_DOMAIN_ID =
            "select id, 'U' as `type` from user where " +
                    "domain_id = ? " +
                    "union " +
                    "select id, 'F' as `type` from federated_user_rax where " +
                    "domain_id = ? " +
                    "order by id " +
                    "limit ? offset ?";

    private static final String COUNT_ENABLED =
            "select count(*) from ( " +
                    "select id from user where " +
                    "enabled = '1' " +
                    "union " +
                    "select id from federated_user_rax) as u";

    private static final String ALL_ENABLED =
            "select id, 'U' as `type` from user where " +
                    "enabled = '1' " +
                    "union " +
                    "select id, 'F' as `type` from federated_user_rax " +
                    "order by id " +
                    "limit ? offset ?";

    @Override
    public PaginatorContext<EndUser> getEndUsersByDomainIdPaged(String domainId, int offset, int limit) {
        final BigInteger count = (BigInteger) entityManager.createNativeQuery(COUNT_BY_DOMAIN_ID)
                .setParameter(1, domainId)
                .setParameter(2, domainId)
                .getSingleResult();
        final int totalSize = count.intValue();

        List<Object[]> listIds = Collections.EMPTY_LIST;
        if (totalSize > 0) {
            listIds = entityManager.createNativeQuery(ALL_BY_DOMAIN_ID)
                    .setParameter(1, domainId)
                    .setParameter(2, domainId)
                    .setParameter(3, limit)
                    .setParameter(4, offset)
                    .getResultList();
        }

        return convertEndUsersPage(offset, limit, totalSize, listIds);
    }

    @Override
    public PaginatorContext<EndUser> getEnabledEndUsersPaged(int offset, int limit) {
        final BigInteger count = (BigInteger) entityManager.createNativeQuery(COUNT_ENABLED).getSingleResult();
        final int totalSize = count.intValue();

        List<Object[]> listIds = Collections.EMPTY_LIST;
        if (totalSize > 0) {
            listIds = entityManager.createNativeQuery(ALL_ENABLED)
                    .setParameter(1, limit)
                    .setParameter(2, offset)
                    .getResultList();
        }

        return convertEndUsersPage(offset, limit, totalSize, listIds);
    }

    private PaginatorContext<EndUser> convertEndUsersPage(int offset, int limit, int count, List<Object[]> listIds) {
        final PaginatorContext<EndUser> result = new PaginatorContext<EndUser>();
        result.setOffset(offset);
        result.setLimit(limit);
        result.setTotalRecords(count);

        if (count > 0) {
            final Set<String> userIds = new HashSet<String>();
            final Set<String> federatedIds = new HashSet<String>();
            for (Object[] data : listIds) {
                if (data[1].equals("U")) {
                    userIds.add((String) data[0]);
                } else {
                    federatedIds.add((String) data[0]);
                }
            }

            final List<User> users = userMapper.fromSQL(userRepository.findAll(userIds));
            final List<FederatedUser> federatedUsers = federatedUserRaxMapper.fromSQL(federatedUserRepository.findAll(federatedIds));
            final Set<EndUser> sorted = new TreeSet<EndUser>(new Comparator<EndUser>() {
                @Override
                public int compare(EndUser o1, EndUser o2) {
                    return o1.getId().compareTo(o2.getId());
                }
            });
            sorted.addAll(users);
            sorted.addAll(federatedUsers);

            result.setValueList(new ArrayList<EndUser>(sorted));
        }

        return result;
    }

}
