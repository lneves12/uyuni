// @flow
import React from 'react';
import CreatorPanel from "../../../../../../components/panels/CreatorPanel";
import PropertiesForm from "./properties-form";
import {Loading} from "components/loading/loading";
import useProjectActionsApi from "../../../api/use-project-actions-api";
import {showErrorToastr, showSuccessToastr} from "components/toastr/toastr";
import PropertiesView from "./properties-view";
import _cloneDeep from "lodash/cloneDeep";

// TODO: consider this: https://github.com/mweststrate/immer

import type {projectPropertiesType, projectHistoryEntry} from '../../../type/project.type.js';

type Props = {
  projectId: String,
  properties: projectPropertiesType,
  showDraftVersion: boolean,
  onChange: Function,
  currentHistoryEntry: projectHistoryEntry
};

const PropertiesEdit = (props: Props) => {
  const {onAction, cancelAction, isLoading} = useProjectActionsApi({
    projectId: props.projectId, projectResource: "properties"
  });

  const defaultDraftHistory = {
    version: props.currentHistoryEntry ? props.currentHistoryEntry.version + 1 : 1 ,
    message:'(draft - not built) - Check the colors bellow for all the changes'
  };

  let propertiesToShow = _cloneDeep(props.properties);

  if(props.showDraftVersion) {
    propertiesToShow.historyEntries.unshift(defaultDraftHistory);
  };

  return (
      <CreatorPanel
        id="properties"
        creatingText="Edit"
        panelLevel="2"
        title={t('Project Properties')}
        collapsible
        customIconClass="fa-small"
        onOpen={({ setItem }) => setItem(props.properties)}
        disableOperations={isLoading}
        onSave={({ item, closeDialog }) => {
          return onAction(item, "update")
            .then((editedProject) => {
              closeDialog();
              showSuccessToastr(t("Project properties updated successfully"));
              props.onChange(editedProject)
            })
            .catch((error) => {
              showErrorToastr(error);
            })
        }}
        onCancel={() => cancelAction()}
        renderContent={() => <PropertiesView properties={propertiesToShow}/>}
        renderCreationContent={({ open, item, setItem }) => {

          if (isLoading) {
            return (
              <Loading text='Editing properties..'/>
            )
          }

          return (
            <PropertiesForm
              properties={item}
              onChange={(editedProperties) => setItem(editedProperties)}
              editing
            />
          )
        }}
      />
  );
}

export default PropertiesEdit;
