// @flow
import React, {useState} from 'react';
import {TopPanel} from "components/panels/TopPanel";
import TopPanelButtons from "./top-panel-buttons";
import PropertiesCreate from "../shared/components/panels/properties/properties-create";
import {showErrorToastr} from "components/toastr/toastr";
import withPageWrapper from 'components/general/with-page-wrapper';
import {hot} from 'react-hot-loader';
import useLifecycleActionsApi from "../shared/api/use-lifecycle-actions-api";

const CreateProject = () => {

  const [project, setProject] = useState({
      properties: {
        label: "",
        name: "",
        description: "",
        historyEntries: [],
      }
  });

  const { onAction } = useLifecycleActionsApi({resource: 'projects'});

  return (
          <TopPanel
            title={t('Create a new Content Lifecycle Project')}
            icon="fa-plus"
            button={
              <TopPanelButtons
                onCreate={() =>
                  onAction(project, "create")
                    .then(
                      () => {
                        window.location.href = `/rhn/manager/contentmanagement/project/${project.properties.label || ''}`
                      }
                    )
                    .catch((error) => {
                      showErrorToastr(error);
                    })
                }
              />
            }
          >
            <PropertiesCreate
              properties={project.properties}
              onChange={(newProperties) => setProject({...project, properties: newProperties})}
            />
          </TopPanel>
  )
}

export default hot(module)(withPageWrapper<{}>(CreateProject));
