// @flow

import React, { useState, useEffect } from 'react';
import {TopPanel} from "../../../components/panels/TopPanel";
import PropertiesEdit from "../shared/components/panels/properties/properties-edit";
import { showErrorToastr, showSuccessToastr } from 'components/toastr/toastr';
import _isEmpty from "lodash/isEmpty";
import "./filter.css";
import {DeleteDialog} from "components/dialog/DeleteDialog";
import {ModalButton} from "components/dialog/ModalButton";
import useFilterActionsApi from '../shared/api/use-filter-actions-api';
import withPageWrapper from 'components/general/with-page-wrapper';
import type {filterType} from '../shared/type/filter.type';

type Props = {
  filter: filterType,
  wasFreshlyCreatedMessage?: string,
};

const Filter = (props: Props) => {

  const [filter, setFilter] = useState(props.filter)
  const {onAction} = useFilterActionsApi({ filterId: filter.properties.label });


  useEffect(()=> {
    if(props.wasFreshlyCreatedMessage) {
      showSuccessToastr(props.wasFreshlyCreatedMessage)
    }
  }, [])

  const editedStates = ["0","2","3"];
  const statesDesc = {
    "0": "added",
    "1": "built",
    "2": "edited",
    "3": "deleted",
  };

  const filterId = filter.properties.label;

  const changesToBuild = filter.sources
    .filter(source => editedStates.includes(source.state))
    .map(source => ({type: "Source", name: source.name, state: statesDesc[source.state]}));

  return (
    <TopPanel
      title={t('Create a new Filter')}
      // icon="fa-plus"
      button= {
        <div className="pull-right btn-group">
          <ModalButton
            className="btn-danger"
            title={t("Delete")}
            text={t('Delete')}
            target="delete-filter-modal" />
        </div>
      }
    >
      <DeleteDialog id="delete-filter-modal"
          title={t("Delete Filter")}
          content={<span>{t("Are you sure you want to delete filter")} <strong>{filterId}</strong>?</span>}
          onConfirm={() =>
            onAction(filter, 'delete')
            .then(() => {
              window.location.href = `/rhn/manager/contentmanagement/filters`
            })
            .catch((error) => {
              showErrorToastr(error);
            })
          }
      />
      <PropertiesEdit
        filterId={filterId}
        properties={filter.properties}
        onChange={(filterWithNewProperties) => {
          setFilter(filterWithNewProperties)
        }}
      />
    </TopPanel>
  );
}

export default withPageWrapper<Props>(Filter);
