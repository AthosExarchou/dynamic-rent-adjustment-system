import React, { useEffect, useState } from 'react';
import { Users } from 'lucide-react';
import apiClient from '../../../shared/api/client';
import { useToast } from '../../../shared/components/Toast';
import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import styles from './AdminDashboard.module.css';
import EditUserModal from './EditUserModal';
import AdminProfileCreationModal from './AdminProfileCreationModal';
import CreateUserModal from './CreateUserModal';

export default function UserManagement() {
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);
  
  // Modal states
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [creatingProfileFor, setCreatingProfileFor] = useState(null); // { userId, roleType }
  
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [userToDelete, setUserToDelete] = useState(null);
  const toast = useToast();

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

  const handleAddRole = async (userId, roleId) => {
    const role = roles.find(r => r.id === roleId);
    try {
      await apiClient(`/user/role/add/${userId}/${roleId}`, { method: 'POST' });
      toast.success(`Role ${role?.name || ''} added successfully.`);
      fetchUsersAndRoles();
    } catch (err) {
      // If backend says profile required, open profile creation modal
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
      
      <div className={styles.tableContainer}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>Username</th>
              <th>Email Address</th>
              <th>Assigned Roles</th>
              <th className={styles.centerAlign}>Manage Roles / Actions</th>
            </tr>
          </thead>
          <tbody>
            {users.map(u => (
              <tr key={u.id}>
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
      </div>

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
