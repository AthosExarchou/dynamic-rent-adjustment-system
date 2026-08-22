import React, { useEffect, useState } from 'react';
import apiClient from '../../../shared/api/client';
import styles from './AdminDashboard.module.css';
import EditUserModal from './EditUserModal';
import AdminProfileCreationModal from './AdminProfileCreationModal';

export default function UserManagement() {
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);
  
  // Modal states
  const [editingUser, setEditingUser] = useState(null);
  const [creatingProfileFor, setCreatingProfileFor] = useState(null); // { userId, roleType }
  
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [userToDelete, setUserToDelete] = useState(null);

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
        { id: 2, name: 'OWNER' },
        { id: 3, name: 'TENANT' }
      ]);
      setError(null);
    } catch (err) {
      if (err.name === 'AbortError') return;
      console.error("Failed to fetch users:", err);
      setUsers([]);
      setRoles([
        { id: 1, name: 'USER' },
        { id: 2, name: 'OWNER' },
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
      fetchUsersAndRoles();
    } catch (err) {
      // If backend says profile required, open profile creation modal
      if (err.message && err.message.includes('PROFILE_REQUIRED')) {
         setCreatingProfileFor({ userId, roleType: role.name });
      } else {
         alert('Failed to add role. Please try again.');
         fetchUsersAndRoles();
      }
    }
  };

  const handleRemoveRole = async (userId, roleId) => {
    try {
      await apiClient(`/user/role/delete/${userId}/${roleId}`, { method: 'POST' });
      fetchUsersAndRoles();
    } catch {
      alert('Failed to remove role.');
      fetchUsersAndRoles();
    }
  };

  const confirmDeleteUser = (userId) => setUserToDelete(userId);

  const handleDeleteUser = async () => {
    if (!userToDelete) return;
    try {
      await apiClient(`/user/delete/${userToDelete}`, { method: 'POST' });
      fetchUsersAndRoles();
      setUserToDelete(null);
    } catch {
      alert('Failed to delete user.');
      setUserToDelete(null);
    }
  };

  return (
    <div className={styles.container}>
      <h2 className={styles.title}>User Management Dashboard</h2>
      
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
                <td>{u.roles.map(r => r.name).join(', ')}</td>
                <td>
                  <div className={styles.roleActions}>
                    <button onClick={() => setEditingUser(u)} className={styles.editBtn}>Edit</button>
                    {roles.map(r => {
                      const hasRole = u.roles.some(ur => ur.name === r.name);
                      return hasRole ? (
                        <button key={r.id} onClick={() => handleRemoveRole(u.id, r.id)} className={styles.removeBtn}>- {r.name}</button>
                      ) : (
                        <button key={r.id} onClick={() => handleAddRole(u.id, r.id)} className={styles.addBtn}>+ {r.name}</button>
                      );
                    })}
                    <button onClick={() => confirmDeleteUser(u.id)} className={styles.delBtn}>Delete</button>
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

      {creatingProfileFor && (
        <AdminProfileCreationModal
          userId={creatingProfileFor.userId}
          roleType={creatingProfileFor.roleType}
          onClose={() => setCreatingProfileFor(null)}
          onRefresh={fetchUsersAndRoles}
        />
      )}

      {userToDelete && (
        <div style={{position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0,0,0,0.5)',
          display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1000}}>
          <div style={{backgroundColor: 'white', padding: '2rem', borderRadius: '8px', maxWidth: '400px', color: '#333'}}>
            <h3 style={{marginTop: 0}}>Confirm Deletion</h3>
            <p>Are you sure you want to delete this user? This action cannot be undone.</p>
            <div style={{display: 'flex', gap: '1rem', marginTop: '1.5rem', justifyContent: 'flex-end'}}>
              <button onClick={() => setUserToDelete(null)} className={styles.addBtn} style={{background: '#ccc', color: '#333'}}>Cancel</button>
              <button onClick={handleDeleteUser} className={styles.delBtn}>Confirm Delete</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
