package com.rackspace.idm.domain.sql.mapper.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.entity.Question;
import com.rackspace.idm.domain.sql.entity.SqlQuestion;
import com.rackspace.idm.domain.sql.mapper.SqlMapper;

@SQLComponent
public class QuestionMapper extends SqlMapper<Question, SqlQuestion> {
}
