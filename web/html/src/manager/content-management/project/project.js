// @flow

import React, { useState, useEffect } from 'react';
import {TopPanel} from "../../../components/panels/TopPanel";
import Sources from "../shared/components/panels/sources/sources";
import PropertiesEdit from "../shared/components/panels/properties/properties-edit";
import Build from "../shared/components/panels/build/build";
import EnvironmentLifecycle from "../shared/components/panels/environment-lifecycle/environment-lifecycle";
import {handleSourcesChange, handleBuild} from "../shared/state/project/project.state";
import { showErrorToastr, showSuccessToastr } from 'components/toastr/toastr';
import Filters from "../shared/components/panels/filters/filters";
import _isEmpty from "lodash/isEmpty";
import "./project.css";
import {DeleteDialog} from "components/dialog/DeleteDialog";
import {ModalButton} from "components/dialog/ModalButton";
import useProjectActionsApi from '../shared/api/use-project-actions-api';
import withPageWrapper from 'components/general/with-page-wrapper';

import type {projectType} from '../shared/type/project.type';

type Props = {
  project: projectType,
  wasFreshlyCreatedMessage?: string,
};

//TODO:  React 16 -> Default error handling implement

const Project = (props: Props) => {

  const [project, setProject] = useState(props.project)
  const {onAction} = useProjectActionsApi({ projectId: project.properties.label });


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

  const projectId = project.properties.label;

  const changesToBuild = project.sources
    .filter(source => editedStates.includes(source.state))
    .map(source => ({type: "Source", name: source.name, state: statesDesc[source.state]}));

  return (
    <TopPanel
      title={t('Create a new Content Lifecycle Project')}
      // icon="fa-plus"
      button= {
        <div className="pull-right btn-group">
          <ModalButton
            className="btn-danger"
            title={t("Delete")}
            text={t('Delete')}
            target="delete-project-modal" />
          <DeleteDialog id="delete-project-modal"
                        title={t("Delete Store")}
                        content={<span>{t("Are you sure you want to delete project")} <strong>{projectId}</strong>?</span>}
                        onConfirm={() =>
                          onAction(project, 'delete')
                            .then(() => {
                              window.location.href = `/rhn/manager/contentmanagement/projects`
                            })
                            .catch((error) => {
                              showErrorToastr(error);
                            })
                        }
          />
        </div>
      }
    >
      <PropertiesEdit
        projectId={projectId}
        properties={project.properties}
        onChange={(projectWithNewProperties) => {
          setProject(projectWithNewProperties)
        }}
      />

      <div className="panel-group-contentmngt">
        <Sources
          projectId={projectId}
          sources={project.sources}
          onChange={(newSources) => {
            setProject(handleSourcesChange(project, newSources))
          }}
        />
        <Filters/>
      </div>

      <Build
        disabled={_isEmpty(project.environments) || changesToBuild.length < 1}
        versionToBuild={project.properties.historyEntries[0]}
        onBuild={(builtVersion) => {
          setProject(handleBuild(project, builtVersion))
          showSuccessToastr(`Version ${builtVersion.version} successfully built into ${project.environments[0].name}`)
        }}
        changesToBuild={changesToBuild}
      />

      <EnvironmentLifecycle
        projectId={projectId}
        environments={project.environments}
        onChange={(projectWithNewEnvironment) => {
          setProject(projectWithNewEnvironment)
        }}
      />
    </TopPanel>
  );
}

export default withPageWrapper<Props>(Project);
