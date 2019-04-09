// @flow
import React, {useState} from 'react';
import useFilterActionsApi from "../shared/api/use-filter-actions-api";
import {TopPanel} from "components/panels/TopPanel";
import TopPanelButtons from "./top-panel-buttons";
import PropertiesCreate from "../shared/components/panels/properties/properties-create";
import {showErrorToastr} from "components/toastr/toastr";
import withPageWrapper from 'components/general/with-page-wrapper';
import {hot} from 'react-hot-loader';

const CreateFilter = () => {

  const [filter, setFilter] = useState({
      properties: {
        name: ""
      }
  });

  const { onAction } = useFilterActionsApi({});

  return (
          <TopPanel
            title={t('Create a new Content Lifecycle Filter')}
            icon="fa-plus"
            button={
              <TopPanelButtons
                onCreate={() =>
                  onAction(filter, "create")
                    .then(
                      () => {
                        window.location.href = `/rhn/manager/contentmanagement/filter/${filter.properties.label || ''}`
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
              properties={filter.properties}
              onChange={(newProperties) => setFilter({...filter, properties: newProperties})}
            />
          </TopPanel>
  )
}

export default hot(module)(withPageWrapper<{}>(CreateFilter));
