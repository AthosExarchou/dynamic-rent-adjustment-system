import React, { useEffect, useState, useMemo } from 'react';
import { Users, Search, Filter, Check, X, SlidersHorizontal, ChevronDown, ChevronUp, Calendar, UserCheck } from 'lucide-react';
import apiClient from '../../../shared/api/client';
import { useToast } from '../../../shared/components/Toast';
import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import DatePicker from '../../../shared/components/DatePicker';
import { Pagination } from '../../../shared/components/Pagination';
import { Select } from '../../../shared/components/Select';
import { InfoTooltip } from '../../../shared/components/InfoTooltip';
import styles from './AdminDashboard.module.css';
import EditUserModal from './EditUserModal';
import AdminProfileCreationModal from './AdminProfileCreationModal';
import CreateUserModal from './CreateUserModal';

const PAGE_SIZE_OPTIONS = [10, 25, 50];

const ROLE_FILTER_OPTIONS = [
  { value: '', label: 'All Roles' },
  { value: 'ADMIN', label: 'Admin' },
  { value: 'OWNER', label: 'Owner' },
  { value: 'TENANT', label: 'Tenant' },
  { value: 'USER', label: 'User' },
];

// Role sort priority: ADMIN first, then OWNER, TENANT, USER
const ROLE_PRIORITY = { ADMIN: 0, OWNER: 1, TENANT: 2, USER: 3 };

function getTopRolePriority(roles) {
  if (!roles || roles.length === 0) return 99;
  return Math.min(...roles.map(r => ROLE_PRIORITY[r] ?? 50));
}

/** "YYYY-MM-DD" of a LocalDateTime ISO string (backend sends "2026-01-15T10:30:00") */
function toDateOnly(isoStr) {
  if (!isoStr) return '';
  return isoStr.slice(0, 10);
}

/** Offset a "YYYY-MM-DD" string by N days */
function offsetDateStr(dateStr, days) {
  if (!dateStr) return '';
  const [y, m, d] = dateStr.split('-').map(Number);
  const dt = new Date(y, m - 1, d);
  dt.setDate(dt.getDate() + days);
  return `${dt.getFullYear()}-${String(dt.getMonth() + 1).padStart(2, '0')}-${String(dt.getDate()).padStart(2, '0')}`;
}

function getTodayStr() {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
}

const EMPTY_FILTER = {
  search: '',
  role: '',
  createdAfter: '',
  createdBefore: '',
  lastLoginAfter: '',
  lastLoginBefore: '',
};

export default function UserManagement() {
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);

  // Filter state (what the user is editing) vs appliedFilter (what's actually filtering the table)
  const [filter, setFilter] = useState(EMPTY_FILTER);
  const [appliedFilter, setAppliedFilter] = useState(EMPTY_FILTER);
  const [isAdvancedOpen, setIsAdvancedOpen] = useState(false);
  const [filterError, setFilterError] = useState(null);

  // Pagination state
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  // Modal states
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [creatingProfileFor, setCreatingProfileFor] = useState(null);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [userToDelete, setUserToDelete] = useState(null);
  const toast = useToast();

  const todayStr = getTodayStr();

  const activeFiltersCount = Object.entries(appliedFilter).filter(([, v]) => v !== '').length;
  const hasActiveFilters = activeFiltersCount > 0;

  useEffect(() => {
    const controller = new AbortController();
    fetchUsersAndRoles(controller.signal);
    return () => controller.abort();
  }, []);

  const fetchUsersAndRoles = async (signal) => {
    setLoading(true);
    try {
      const usersData = await apiClient('/users', signal ? { signal } : {});
      setUsers(usersData);
      setRoles([
        { id: 1, name: 'USER' },
        { id: 4, name: 'OWNER' },
        { id: 3, name: 'TENANT' }
      ]);
      setError(null);
    } catch (err) {
      if (err.name === 'AbortError') return;
      console.error("Failed to fetch users:", err);
      setUsers([]);
      setRoles([
        { id: 1, name: 'USER' },
        { id: 4, name: 'OWNER' },
        { id: 3, name: 'TENANT' }
      ]);
      setError("Failed to load users.");
    } finally {
      setLoading(false);
    }
  };

  // Reset to page 1 whenever the applied filter changes
  useEffect(() => {
    setCurrentPage(1);
  }, [appliedFilter]);

  // Sorted & filtered users (pure client-side, only re-runs on appliedFilter change)
  const displayedUsers = useMemo(() => {
    const q = appliedFilter.search.trim().toLowerCase();
    return [...users]
      .filter(u => {
        // Role filter
        if (appliedFilter.role && !u.roles.includes(appliedFilter.role)) return false;
        // Text search: ID, username, email
        if (q) {
          const match =
            String(u.id).includes(q) ||
            u.username.toLowerCase().includes(q) ||
            u.email.toLowerCase().includes(q);
          if (!match) return false;
        }
        // Created After / Before
        if (appliedFilter.createdAfter && toDateOnly(u.createdAt) < appliedFilter.createdAfter) return false;
        if (appliedFilter.createdBefore && toDateOnly(u.createdAt) > appliedFilter.createdBefore) return false;
        // Last Login After / Before
        if (appliedFilter.lastLoginAfter && toDateOnly(u.lastLogin) < appliedFilter.lastLoginAfter) return false;
        if (appliedFilter.lastLoginBefore && toDateOnly(u.lastLogin) > appliedFilter.lastLoginBefore) return false;
        return true;
      })
      // Sort: by top role priority, then alphabetically by username
      .sort((a, b) => {
        const roleDiff = getTopRolePriority(a.roles) - getTopRolePriority(b.roles);
        if (roleDiff !== 0) return roleDiff;
        return a.username.localeCompare(b.username);
      });
  }, [users, appliedFilter]);

  // Paginated slice shown in the table
  const paginatedUsers = useMemo(
    () => displayedUsers.slice((currentPage - 1) * pageSize, currentPage * pageSize),
    [displayedUsers, currentPage, pageSize]
  );

  const handleFilterChange = (key, val) => setFilter(prev => ({ ...prev, [key]: val }));

  const handleFilterSubmit = (e) => {
    e.preventDefault();
    setFilterError(null);

    // Date range validations
    if (filter.createdAfter && filter.createdAfter > todayStr) {
      setFilterError('Created After date cannot be in the future.'); return;
    }
    if (filter.createdBefore && filter.createdBefore > todayStr) {
      setFilterError('Created Before date cannot be in the future.'); return;
    }
    if (filter.createdAfter && filter.createdBefore && filter.createdAfter >= filter.createdBefore) {
      setFilterError('Created After must be earlier than Created Before.'); return;
    }
    if (filter.lastLoginAfter && filter.lastLoginAfter > todayStr) {
      setFilterError('Last Login After date cannot be in the future.'); return;
    }
    if (filter.lastLoginBefore && filter.lastLoginBefore > todayStr) {
      setFilterError('Last Login Before date cannot be in the future.'); return;
    }
    if (filter.lastLoginAfter && filter.lastLoginBefore && filter.lastLoginAfter >= filter.lastLoginBefore) {
      setFilterError('Last Login After must be earlier than Last Login Before.'); return;
    }

    setAppliedFilter({ ...filter });
  };

  const handleFilterReset = () => {
    setFilterError(null);
    setFilter(EMPTY_FILTER);
    setAppliedFilter(EMPTY_FILTER);
  };

  const handleAddRole = async (userId, roleId) => {
    const role = roles.find(r => r.id === roleId);
    try {
      await apiClient(`/user/role/add/${userId}/${roleId}`, { method: 'POST' });
      toast.success(`Role ${role?.name || ''} added successfully.`);
      fetchUsersAndRoles();
    } catch (err) {
      if (err.message && err.message.includes('PROFILE_REQUIRED')) {
         setCreatingProfileFor({ userId, roleType: role.name });
      } else {
         toast.error(err.message || 'Failed to add role. Please try again.');
         fetchUsersAndRoles();
      }
    }
  };

  const handleRemoveRole = async (userId, roleId) => {
    try {
      await apiClient(`/user/role/delete/${userId}/${roleId}`, { method: 'POST' });
      toast.success('Role removed successfully.');
      fetchUsersAndRoles();
    } catch (err) {
      toast.error(err.message || 'Failed to remove role.');
      fetchUsersAndRoles();
    }
  };

  const confirmDeleteUser = (userId) => setUserToDelete(userId);

  const handleDeleteUser = async () => {
    if (!userToDelete) return;
    const targetUserId = userToDelete;
    setUserToDelete(null);
    try {
      await apiClient(`/user/delete/${targetUserId}`, { method: 'POST' });
      toast.success('User deleted successfully.');
      fetchUsersAndRoles();
    } catch (err) {
      toast.error(err.message || 'Failed to delete user.');
    }
  };

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <h2 className={styles.title}>
          <Users className={styles.titleIcon} size={28} />
          User Management Dashboard
        </h2>
        <button
          className={styles.approveBtn}
          onClick={() => setShowCreateModal(true)}
          style={{ padding: '0.4rem 1rem' }}
        >
          + Create User
        </button>
      </div>

      <hr className={styles.divider} />

      {/* Filter Card */}
      <div className={styles.filterCard}>
        <div className={styles.filterHeader}>
          <h5 className={styles.filterTitle}>
            <Filter size={20} />
            Filter Users
            {activeFiltersCount > 0 && (
              <span style={{ fontSize: '0.85rem', opacity: 0.9 }}>({activeFiltersCount} active)</span>
            )}
          </h5>
        </div>
        <div className={styles.filterBody}>
          <form onSubmit={handleFilterSubmit} className={styles.filterForm}>

            {/* Search */}
            <div className={styles.formGroup}>
              <label className={styles.label}>
                <Search size={16} /> Search
              </label>
              <input
                type="text"
                className={styles.input}
                placeholder="ID, username or email..."
                value={filter.search}
                onChange={e => handleFilterChange('search', e.target.value)}
              />
            </div>

            {/* Role */}
            <div className={styles.formGroup}>
              <label className={styles.label}>
                <UserCheck size={16} /> Role
              </label>
              <Select
                value={filter.role}
                onChange={val => handleFilterChange('role', val)}
                options={ROLE_FILTER_OPTIONS}
                placeholder="All Roles"
              />
            </div>

            {/* Advanced toggle */}
            <div className={styles.advancedToggleWrapper}>
              <button
                type="button"
                className={styles.advancedToggleBtn}
                onClick={() => setIsAdvancedOpen(o => !o)}
              >
                <SlidersHorizontal size={16} />
                {isAdvancedOpen ? 'Hide Date Filters' : 'More Filters (Account Created, Last Login)'}
                {isAdvancedOpen ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
              </button>
            </div>

            {/* Advanced: date ranges */}
            {isAdvancedOpen && (
              <div className={styles.advancedGrid}>
                {/* Created date range */}
                <div className={styles.formGroup}>
                  <label className={styles.label}>
                    <Calendar size={16} /> Account Created After
                  </label>
                  <DatePicker
                    value={filter.createdAfter}
                    onChange={val => {
                      if (val && filter.createdBefore && val >= filter.createdBefore) {
                        setFilter(prev => ({ ...prev, createdAfter: val, createdBefore: '' }));
                      } else {
                        handleFilterChange('createdAfter', val);
                      }
                    }}
                    placeholder="Select start date"
                    max={
                      filter.createdBefore
                        ? (offsetDateStr(filter.createdBefore, -1) < todayStr ? offsetDateStr(filter.createdBefore, -1) : todayStr)
                        : todayStr
                    }
                  />
                </div>

                <div className={styles.formGroup}>
                  <label className={styles.label}>
                    <Calendar size={16} /> Account Created Before
                  </label>
                  <DatePicker
                    value={filter.createdBefore}
                    onChange={val => {
                      if (val && filter.createdAfter && val <= filter.createdAfter) {
                        setFilter(prev => ({ ...prev, createdBefore: val, createdAfter: '' }));
                      } else {
                        handleFilterChange('createdBefore', val);
                      }
                    }}
                    placeholder="Select end date"
                    min={filter.createdAfter ? offsetDateStr(filter.createdAfter, 1) : undefined}
                    max={todayStr}
                    align="right"
                  />
                </div>

                {/* Last Login date range */}
                <div className={styles.formGroup}>
                  <label className={styles.label}>
                    <Calendar size={16} /> Last Login After
                  </label>
                  <DatePicker
                    value={filter.lastLoginAfter}
                    onChange={val => {
                      if (val && filter.lastLoginBefore && val >= filter.lastLoginBefore) {
                        setFilter(prev => ({ ...prev, lastLoginAfter: val, lastLoginBefore: '' }));
                      } else {
                        handleFilterChange('lastLoginAfter', val);
                      }
                    }}
                    placeholder="Select start date"
                    max={
                      filter.lastLoginBefore
                        ? (offsetDateStr(filter.lastLoginBefore, -1) < todayStr ? offsetDateStr(filter.lastLoginBefore, -1) : todayStr)
                        : todayStr
                    }
                  />
                </div>

                <div className={styles.formGroup}>
                  <label className={styles.label}>
                    <Calendar size={16} /> Last Login Before
                  </label>
                  <DatePicker
                    value={filter.lastLoginBefore}
                    onChange={val => {
                      if (val && filter.lastLoginAfter && val <= filter.lastLoginAfter) {
                        setFilter(prev => ({ ...prev, lastLoginBefore: val, lastLoginAfter: '' }));
                      } else {
                        handleFilterChange('lastLoginBefore', val);
                      }
                    }}
                    placeholder="Select end date"
                    min={filter.lastLoginAfter ? offsetDateStr(filter.lastLoginAfter, 1) : undefined}
                    max={todayStr}
                    align="right"
                  />
                </div>
              </div>
            )}

            {filterError && (
              <p className={styles.filterError}>{filterError}</p>
            )}

            <div className={styles.filterBtnWrapper}>
              <button type="submit" className={styles.filterApplyBtn}>
                <Check size={16} /> Apply Filters {activeFiltersCount > 0 && `(${activeFiltersCount})`}
              </button>
              {hasActiveFilters && (
                <button type="button" onClick={handleFilterReset} className={styles.filterResetBtn}>
                  <X size={16} /> Reset
                </button>
              )}
            </div>

          </form>
        </div>
      </div>

      <div className={styles.tableContainer}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>ID</th>
              <th>Username</th>
              <th>Email Address</th>
              <th>
                Assigned Roles
                <InfoTooltip text="Platform access permissions. Assigning the Owner role requires an existing Owner profile with contact details." />
              </th>
              <th className={styles.centerAlign}>Manage Roles / Actions</th>
            </tr>
          </thead>
          <tbody>
            {paginatedUsers.map(u => (
              <tr key={u.id}>
                <td className={styles.idCell}>{u.id}</td>
                <td>{u.username}</td>
                <td>{u.email}</td>
                <td>{u.roles.join(', ')}</td>
                <td>
                  <div className={styles.roleActions}>
                    {!u.roles.includes('ADMIN') && !u.isSystem && (
                      <>
                        <button onClick={() => setEditingUser(u)} className={styles.editBtn}>Edit</button>
                        {roles.map(r => {
                          const hasRole = u.roles.some(ur => ur === r.name);
                          return hasRole ? (
                            <button key={r.id} onClick={() => handleRemoveRole(u.id, r.id)} className={styles.removeBtn}>- {r.name}</button>
                          ) : (
                            <button key={r.id} onClick={() => handleAddRole(u.id, r.id)} className={styles.addBtn}>+ {r.name}</button>
                          );
                        })}
                        <button onClick={() => confirmDeleteUser(u.id)} className={styles.delBtn}>Delete</button>
                      </>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {loading && <p style={{textAlign: 'center', padding: '1rem'}}>Loading users...</p>}
        {error && <p style={{textAlign: 'center', padding: '1rem', color: 'red'}}>{error}</p>}
        {!loading && !error && users.length === 0 && <p style={{textAlign: 'center', padding: '1rem'}}>No users found.</p>}
        {!loading && !error && users.length > 0 && displayedUsers.length === 0 && (
          <p style={{textAlign: 'center', padding: '1rem', color: 'var(--text-secondary)'}}>
            No users match your filters.
          </p>
        )}
      </div>

      {!loading && !error && displayedUsers.length > 0 && (
        <Pagination
          currentPage={currentPage}
          totalItems={displayedUsers.length}
          pageSize={pageSize}
          onPageChange={setCurrentPage}
          pageSizeOptions={PAGE_SIZE_OPTIONS}
          onPageSizeChange={(newSize) => {
            setPageSize(newSize);
            setCurrentPage(1);
          }}
        />
      )}

      <EditUserModal
        user={editingUser}
        onClose={() => setEditingUser(null)}
        onRefresh={fetchUsersAndRoles}
      />

      {showCreateModal && (
        <CreateUserModal
          onClose={() => setShowCreateModal(false)}
          onRefresh={fetchUsersAndRoles}
        />
      )}

      {creatingProfileFor && (
        <AdminProfileCreationModal
          userId={creatingProfileFor.userId}
          roleType={creatingProfileFor.roleType}
          onClose={() => setCreatingProfileFor(null)}
          onRefresh={fetchUsersAndRoles}
        />
      )}

      {userToDelete && (
        <ConfirmDialog
          title="Confirm Deletion"
          message="Are you sure you want to delete this user? This action cannot be undone."
          confirmLabel="Confirm Delete"
          variant="danger"
          onConfirm={handleDeleteUser}
          onCancel={() => setUserToDelete(null)}
        />
      )}
    </div>
  );
}
