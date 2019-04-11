//@flow
import React from 'react';
import {Text} from "components/input/Text";
import {Select} from "components/input/Select";
import {Form} from "components/input/Form";
import {Check} from "components/input/Check";

import type {FilterType} from '../../../type/filter.type.js';

type Props = {
  filter: FilterType,
  onChange: Function,
  editing?: boolean
}

const FilterForm = (props: Props) =>
  <Form
    model={props.filter}
    onChange={model => {
      props.onChange(model);
    }}
  >
    <React.Fragment>
      <div className="row">
        <Text
          name="name"
          label={t("Name")}
          labelClass="col-md-3"
          divClass="col-md-8"/>
      </div>
      <div className="row">
        <Select
          name="target"
          label={t("Filter Target")}
          labelClass="col-md-3"
          divClass="col-md-8">
          <option key={'package'} value={'package'}>{t('package')}</option>
          <option key={'patch'} value={'patch'}>{t('patch')}</option>
        </Select>
      </div>
      <div className="row">
        <Text
          name="criteria"
          label={t("Criteria")}
          labelClass="col-md-3"
          divClass="col-md-8"/>
      </div>
      <div className="row">
        <Check
          name="deny"
          label={t("deny")}
          labelClass="col-md-3"
          divClass="col-md-8"/>
      </div>
    </React.Fragment>
  </Form>


export default FilterForm;