// @flow

//TODO: change first letter to UpperCase

export type projectHistoryEntry = {
  version: number,
  message?: string
}

export type projectPropertiesType = {
  label: String,
  name: String,
  description?: String,
  historyEntries: Array<projectHistoryEntry>,
}

export type projectSoftwareSourceType = {
  id: String,
  name: String,
  label: String,
  state: String,
  type: String,
}

export type projectEnvironmentType = {
  projectLabel: String,
  label: String,
  name: String,
  description: String,
}


export type projectType = {
  properties: projectPropertiesType,
  softwareSources: Array<projectSoftwareSourceType>,
  filters: Array<any>,
  environments: Array<projectEnvironmentType>
}
