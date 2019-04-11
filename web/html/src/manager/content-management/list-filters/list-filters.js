// @flow
import React, {useEffect} from 'react';
import {TopPanel} from 'components/panels/TopPanel';
import {Table, Column, SearchField} from 'components/table';
import Functions from 'utils/functions';
import {LinkButton} from 'components/buttons';
import { showSuccessToastr } from 'components/toastr/toastr';
import withPageWrapper from 'components/general/with-page-wrapper';
import FilterForm from '../shared/components/panels/filters/filter-form';
import type {FilterType} from '../shared/type/filter.type.js';

type Props = {
  filters: Array<FilterType>,
  flashMessage: String,
};

const ListFilters = (props: Props) => {

  useEffect(()=> {
    if(props.flashMessage) {
      showSuccessToastr(props.flashMessage)
    }
  }, [])

  const searchData = (row, criteria) => {
    const keysToSearch = ['name'];
    if (criteria) {
      return keysToSearch.map(key => row[key]).join().toLowerCase().includes(criteria.toLowerCase());
    }
    return true;
  };

  const normalizedFilters = props.filters.map(filter => ({
    name: filter.name,
    target: filter.target,
    criteria: filter.criteria,
    deny: filter.deny,
    projects: filter.projects
  }));

  const panelButtons = (
    <div className="pull-right btn-group">
      <LinkButton
        id="createfilter"
        icon="fa-plus"
        className="btn-link"
        title={t('Create a new filter for content lifecycle project')}
        text={t('Create Filter')}
        href="/rhn/manager/contentmanagement/filter"
      />
    </div>
  );

  return (
      <TopPanel title={t('Filter')} icon="spacewalk-icon-software-channels" button={panelButtons}>
        <Table
          data={normalizedFilters}
          identifier={row => row.name}
          searchField={(
            <SearchField
              filter={searchData}
              placeholder={t('Filter by any value')}
            />
          )}
        >
          <Column
            columnKey="name"
            comparator={Functions.Utils.sortByText}
            header={t('Name')}
            cell={row =>
              <a href={`/rhn/manager/contentmanagement/filter/${row.name}`}>
                {row.name}
              </a>
            }
          />
          <Column
            columnKey="projects"
            header={t('Projects in use')}
            cell={row => row.projects.map(p =>
              <a className="project-tag-link" href={`/rhn/manager/contentmanagement/project/${p}`}>
                {p}
              </a>
              )}
          />
        </Table>
        <FilterForm
          filter={normalizedFilters[0]}
          onChange={(item) => setItem(item)}/>
      </TopPanel>
    );
}

export default withPageWrapper<Props>(ListFilters);
